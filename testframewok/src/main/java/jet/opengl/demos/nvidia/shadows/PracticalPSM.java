package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectInstance;

import jet.opengl.demos.scene.CameraData;
import jet.opengl.demos.scene.SceneConfig;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/11/8.
 */

final class PracticalPSM {
    static float ZFAR_MAX = 800.0f;
    static float ZNEAR_MIN = 1.f;
    static float W_EPSILON = 0.001f;
    static float SMQUAD_SIZE = 800.00f;

    // data members
    private float   m_time;
    private float   m_startTime;
    private float   m_fps;
    private int     m_frame;
//    LPD3DXEFFECT m_pEffect;

    private final Vector3f m_lightDir = new Vector3f();

//    LPDIRECT3DSURFACE9 m_pBackBuffer, m_pZBuffer;
//    LPDIRECT3DSURFACE9 m_pSMColorSurface, m_pSMZSurface;
//    LPDIRECT3DTEXTURE9 m_pSMColorTexture, m_pSMZTexture;
//    IDirect3DVertexDeclaration9* m_pDeclaration;

//    SMMeshInfo m_smBigship, m_smQuad;
    float m_fNumStdDevs;

    //  near & far scene clip planes
    final List<BoundingBox> m_ShadowCasterPoints = new ArrayList<>();
    final List<BoundingBox> m_ShadowReceiverPoints = new ArrayList<>();
    final StackInt m_ShadowCasterObjects = new StackInt();
    final StackInt m_ShadowReceiverObjects = new StackInt();
//    D3DXATTRIBUTERANGE*  m_pAttributes;
    // Transforms
    private final Matrix4f m_LightViewProj = new Matrix4f();

//    bool m_bSupportsPixelShaders20;
//    bool m_bSupportsHWShadowMaps;

//    std::vector<ObjectInstance*> m_Scenes;
//    NVBScene* m_pRockChunk;
//    NVBScene* m_pClawBot;
    final List<BoundingBox> m_vClawBotLocalBBs = new ArrayList<>();
    final List<BoundingBox> m_vRockChunkLocalBBs = new ArrayList<>();
    BoundingBox m_ClawBotLocalBB;
    BoundingBox m_RockChunkLocalBB;
    BoundingBox m_VisibleScene;
    float m_fSlideBack = 0.f;
    boolean m_bUnitCubeClip = true;
    float m_ppNear, m_ppFar;
    float m_fCosGamma;
    float m_fLSPSM_Nopt;
    float m_fLSPSM_NoptWeight;
    float m_zFar;
    float m_zNear;

    //bit depth of shadow map
    int m_bitDepth;
    private ShadowSceneController m_Scenes;

    void GetTerrainBoundingBox( List<BoundingBox> shadowReceivers, Matrix4f modelView, Frustum sceneFrustum )
    {
        Vector3f smq_start = new Vector3f(-SMQUAD_SIZE, -10.f, -SMQUAD_SIZE);
        Vector3f smq_width = new Vector3f(2.f*SMQUAD_SIZE, 0.f, 0.f);
        Vector3f smq_height = new Vector3f(0.f, 0.f, 2.f*SMQUAD_SIZE);

        for (int k=0; k<4*4; k++)
        {
            float kx = (k&0x3);
            float ky = ((k>>2)&0x3);
            BoundingBox hugeBox = new BoundingBox();
            /*hugeBox.minPt = smq_start + (kx/4.f)*smq_width + (ky/4.f)*smq_height;
            hugeBox.maxPt = smq_start + ((kx+1.f)/4.f)*smq_width + ((ky+1.f)/4.f)*smq_height;*/
            Vector3f.linear(smq_width, kx/4.f, smq_height, ky/4.f, hugeBox._min);   Vector3f.add(hugeBox._min,smq_start, hugeBox._min);
            Vector3f.linear(smq_width, (kx+1.f)/4.f, smq_height, (ky+1.f)/4.f, hugeBox._max);   Vector3f.add(hugeBox._max,smq_start, hugeBox._max);

            int hugeResult = sceneFrustum.TestBox(hugeBox);
            if ( hugeResult !=2 )  //  2 requires more testing...  0 is fast reject, 1 is fast accept
            {
                if ( hugeResult == 1 )
                {
//                    XFormBoundingBox(&hugeBox, &hugeBox, modelView);
                    BoundingBox.transform(modelView, hugeBox, hugeBox);
                    shadowReceivers.add(hugeBox);
                }
                continue;
            }


            for (int j=0; j<4*4; j++)
            {
                float jx = kx*4.f + (j&0x3);
                float jy = ky*4.f + ((j>>2)&0x3);
                BoundingBox bigBox = new BoundingBox();
                /*bigBox.minPt = smq_start + (jx/16.f)*smq_width + (jy/16.f)*smq_height;
                bigBox.maxPt = smq_start + ((jx+1.f)/16.f)*smq_width + ((jy+1.f)/16.f)*smq_height;*/
                Vector3f.linear(smq_width, jx/16.f, smq_height, jy/16.f, bigBox._min);   Vector3f.add(bigBox._min,smq_start, bigBox._min);
                Vector3f.linear(smq_width, (jx+1.f)/16.f, smq_height, (jy+1.f)/16.f, bigBox._max);   Vector3f.add(bigBox._max,smq_start, bigBox._max);
                int bigResult = sceneFrustum.TestBox(bigBox);
                if ( bigResult != 2 )
                {
                    if ( bigResult == 1 )
                    {
//                        XFormBoundingBox(&bigBox, &bigBox, modelView);
                        BoundingBox.transform(modelView, bigBox, bigBox);
                        shadowReceivers.add(bigBox);
                    }
                    continue;
                }

                for (int q=0; q<4; q++)
                {
                    float iy = jy*4.f + q;
                    int stack = 0;

                    for (int r=0; r<4; r++)
                    {
                        float ix = jx*4.f + (r);
                        BoundingBox smallBox = new BoundingBox();
                        /*smallBox.minPt = smq_start + (ix/64.f)*smq_width + (iy/64.f)*smq_height;
                        smallBox.maxPt = smq_start + ((ix+1.f)/64.f)*smq_width + ((iy+1.f)/64.f)*smq_height;*/
                        Vector3f.linear(smq_width, ix/64.f, smq_height, iy/64.f, smallBox._min);   Vector3f.add(smallBox._min,smq_start, smallBox._min);
                        Vector3f.linear(smq_width, (ix+1.f)/64.f, smq_height, (iy+1.f)/64.f, smallBox._max);   Vector3f.add(smallBox._max,smq_start, smallBox._max);
                        if (sceneFrustum.TestBox(smallBox) != 0)
                        {
                            stack |= (1 << r);
                        }
                    }

                    if (stack != 0)
                    {
                        float firstX =0, lastX = 0;
                        int i;
                        for (i=0; i<4; i++)
                        {
                            if ( (stack&(1<<i)) != 0)
                            {
                                firstX = (i);
                                break;
                            }
                        }
                        for (i=3; i>=0; i--)
                        {
                            if ( (stack&(1<<i)) !=0)
                            {
                                lastX = (i);
                                break;
                            }
                        }
                        firstX += jx*4.f;
                        lastX  += jx*4.f;

                        BoundingBox coalescedBox = new BoundingBox();
                        /*coalescedBox.minPt = smq_start + (firstX/64.f)*smq_width + (iy/64.f)*smq_height;
                        coalescedBox.maxPt = smq_start + ((lastX+1.f)/64.f)*smq_width + ((iy+1.f)/64.f)*smq_height;*/
                        Vector3f.linear(smq_width, firstX/64.f, smq_height, iy/64.f, coalescedBox._min);   Vector3f.add(coalescedBox._min,smq_start, coalescedBox._min);
                        Vector3f.linear(smq_width, (lastX+1.f)/64.f, smq_height, (iy+1.f)/64.f, coalescedBox._max);   Vector3f.add(coalescedBox._max,smq_start, coalescedBox._max);
//                        XFormBoundingBox(&coalescedBox, &coalescedBox, modelView);
                        BoundingBox.transform(modelView, coalescedBox, coalescedBox);
                        shadowReceivers.add(coalescedBox);
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------------
    //  PracticalPSM::ComputeVirtualCameraParameters( )
    //    computes the near & far clip planes for the virtual camera based on the
    //    scene.
    //
    //    bounds the field of view for the virtual camera based on a swept-sphere/frustum
    //    intersection.  if the swept sphere representing the extrusion of an object's bounding
    //    sphere along the light direction intersects the view frustum, the object is added to
    //    a list of interesting shadow casters.  the field of view is the minimum cone containing
    //    all eligible bounding spheres.
    //-----------------------------------------------------------------------------
    void ComputeVirtualCameraParameters(CameraData scenes)
    {
        boolean hit = false;

        //  frustum is in world space, so that bounding boxes are minimal size (xforming an AABB
        //  generally increases its volume).
        /*D3DXMATRIX modelView;
        D3DXMATRIX modelViewProjection;
        D3DXMatrixMultiply(&modelView, &m_World, &m_View);
        D3DXMatrixMultiply(&modelViewProjection, &modelView, &m_Projection);*/
        Matrix4f modelView = scenes.getViewMatrix();
        Matrix4f modelViewProjection = scenes.getViewProjMatrix();

        Vector3f sweepDir = new Vector3f(m_lightDir);
        sweepDir.scale(-1);

        Frustum sceneFrustum = new Frustum(modelViewProjection);

        m_ShadowCasterPoints.clear();
        m_ShadowCasterObjects.clear();
        m_ShadowReceiverObjects.clear();
        m_ShadowReceiverPoints.clear();

        for (int i=0; i<m_Scenes.getShadowCasterCount(); i++)
        {
            /*const ObjectInstance* instance = m_Scenes[i];
            BoundingBox instanceBox = *instance->aabb;
            instanceBox.minPt += instance->translation;
            instanceBox.maxPt += instance->translation;*/
            int instance = i;
            BoundingBox instanceBox = new BoundingBox();
            List<BoundingBox> boxen = m_Scenes.getObjectBoundingBox(i, instanceBox);
            int inFrustum = sceneFrustum.TestBox(instanceBox);  //  0 = outside.  1 = inside.   2 = intersection

            switch (inFrustum)
            {
                case 0:   // outside frustum -- test swept sphere for potential shadow caster
                {
                    BoundingSphere instanceSphere = new BoundingSphere(instanceBox);
                    if (sceneFrustum.TestSweptSphere(instanceSphere, sweepDir))
                    {
                        hit = true;
//                        XFormBoundingBox(&instanceBox, &instanceBox, &modelView);
                        BoundingBox.transform(modelView, instanceBox, instanceBox);
                        m_ShadowCasterPoints.add(instanceBox);
                        m_ShadowCasterObjects.push(instance);
                    }

                    break;
                }
                case 1:  //  fully inside frustum.  so store large bounding box
                {
                    hit = true;
//                    XFormBoundingBox(&instanceBox, &instanceBox, &modelView);
                    BoundingBox.transform(modelView, instanceBox, instanceBox);
                    m_ShadowCasterPoints.add(instanceBox);
                    m_ShadowCasterObjects.push(instance);
                    m_ShadowReceiverPoints.add(instanceBox);
                    m_ShadowReceiverObjects.push(instance);
                    break;
                }
                case 2:   //  big box intersects frustum.  test sub-boxen.  this improves shadow quality, since it allows
                    //  a tighter bounding cone to be used.
                {
                    //  only include objects in list once
                    m_ShadowCasterObjects.push(instance);
                    m_ShadowReceiverObjects.push(instance);
//                    const std::vector<BoundingBox>& boxen = *instance->aaBoxen;
                    for (int box=0; boxen != null && box<boxen.size(); box++)
                    {
                        BoundingBox smallBox = new BoundingBox(boxen.get(box));
                        /*smallBox.minPt += instance->translation;
                        smallBox.maxPt += instance->translation;*/
                        if (sceneFrustum.TestBox(smallBox)!=0)  // at least part of small box is in frustum
                        {
                            hit = true;
//                            XFormBoundingBox(&smallBox, &smallBox, &modelView);
                            BoundingBox.transform(modelView, smallBox, smallBox);
                            m_ShadowCasterPoints.add(smallBox);
                            m_ShadowReceiverPoints.add(smallBox);
                        }
                    }
                    break;
                }
            }
        }

        //  add the biggest shadow receiver -- the ground!
        GetTerrainBoundingBox(m_ShadowReceiverPoints, modelView, sceneFrustum);

        //  these are the limits specified by the physical camera
        //  gamma is the "tilt angle" between the light and the view direction.
        /*m_fCosGamma = m_lightDir.x * m_View._13 +
                m_lightDir.y * m_View._23 +
                m_lightDir.z * m_View._33;*/
        m_fCosGamma = Vector3f.dot(m_lightDir, scenes.getLookAt());

        if (!hit)
        {
            m_zNear = ZNEAR_MIN;
            m_zFar = ZFAR_MAX;
            m_fSlideBack = 0.f;
        }
        else
        {
            float min_z = 1e32f;
            float max_z = 0.f;
            for (int i=0;i < m_ShadowReceiverPoints.size(); i++)
            {
                min_z = Math.min(min_z, m_ShadowReceiverPoints.get(i)._min.z);
                max_z = Math.max(max_z, m_ShadowReceiverPoints.get(i)._max.z);
            }
            /*m_zNear = max(ZNEAR_MIN, min_z);
            m_zFar = min( ZFAR_MAX, max_z );*/
            m_zNear = Math.max(ZNEAR_MIN, -max_z);
            m_zFar = Math.min( ZFAR_MAX,  -min_z);
            m_fSlideBack = 0.f;
        }
    }

    void RenderShadowMap(CameraData sceneData, ShadowGenerator.ShadowMapWarping warping){
        switch (warping){
            case PSM:
                BuildPSMProjectionMatrix(sceneData);
                break;
            case LiSPSM:
                BuildLSPSMProjectionMatrix(sceneData);
                break;
            case TSM:
                BuildTSMProjectionMatrix(sceneData);
                break;
            case ORTHO:
                BuildOrthoShadowProjectionMatrix(sceneData);
                break;
        }
    }

    //-----------------------------------------------------------------------------
    // Name: BuildPSMProjectionMatrix
    // Desc: Builds a perpsective shadow map transformation matrix
    //-----------------------------------------------------------------------------
    void BuildPSMProjectionMatrix(CameraData sceneData)
    {
//        D3DXMATRIX lightView, lightProj, virtualCameraViewProj, virtualCameraView, virtualCameraProj;
        Matrix4f lightView = new Matrix4f();
        Matrix4f lightProj = new Matrix4f();
        Matrix4f virtualCameraViewProj = new Matrix4f();
        Matrix4f virtualCameraView = new Matrix4f();
        Matrix4f virtualCameraProj = new Matrix4f();

        final Vector3f yAxis   = new Vector3f( 0.f, 1.f, 0.f);
        final Vector3f zAxis   = new Vector3f( 0.f, 0.f, 1.f);
        Matrix4f m_View = sceneData.getViewMatrix();

        //  update the virutal scene camera's bounding parameters
        ComputeVirtualCameraParameters(sceneData );

        //  compute a slideback, that force some distance between the infinity plane and the view-box
        final float Z_EPSILON=0.0001f;
        float infinity = sceneData.far/(sceneData.far-sceneData.near);
        float m_fMinInfinityZ = 1.5f;
        float fInfinityZ = m_fMinInfinityZ;
        boolean m_bSlideBack = true;
        if ( (infinity<=fInfinityZ) && m_bSlideBack)
        {
            float additionalSlide = fInfinityZ*(m_zFar-m_zNear) - m_zFar + Z_EPSILON;
            m_fSlideBack = additionalSlide;
            m_zFar += additionalSlide;
            m_zNear += additionalSlide;
        }

        if (m_bSlideBack)
        {
            //  clamp the view-cube to the slid back objects...
            final Vector3f eyePt = new Vector3f(0.f, 0.f, 0.f);
            final Vector3f eyeDir = new Vector3f(0.f, 0.f, -1.f);  // TODO
//            D3DXMatrixTranslation(&virtualCameraView, 0.f, 0.f, m_fSlideBack);
            virtualCameraView.setTranslate(0.f, 0.f, m_fSlideBack);

            if ( m_bUnitCubeClip )
            {
                BoundingCone bc = new BoundingCone(m_ShadowReceiverPoints, virtualCameraView, eyePt, eyeDir );
//                D3DXMatrixPerspectiveLH( &virtualCameraProj, 2.f*tanf(bc.fovx)*m_zNear, 2.f*tanf(bc.fovy)*m_zNear, m_zNear, m_zFar );
//                Matrix4f.perspective(2.f*(float)Math.tan(bc.fovx)*m_zNear, 2.f*(float)Math.tan(bc.fovy)*m_zNear, m_zNear, m_zFar, virtualCameraProj);
                Matrix4f.frustum(2.f*(float)Math.tan(bc.fovx)*m_zNear, 2.f*(float)Math.tan(bc.fovy)*m_zNear, m_zNear, m_zFar, virtualCameraProj);
            }
            else
            {
                final float viewHeight = ZFAR_MAX * 0.57735026919f;  // tan(0.5f*VIEW_ANGLE)*ZFAR_MAX
                float viewWidth  = viewHeight * sceneData.aspect;
                float halfFovy = (float) Math.tan( viewHeight / (ZFAR_MAX+m_fSlideBack) );
                float halfFovx = (float) Math.tan( viewWidth  / (ZFAR_MAX+m_fSlideBack) );

//                D3DXMatrixPerspectiveLH( &virtualCameraProj, 2.f*tanf(halfFovx)*m_zNear, 2.f*tanf(halfFovy)*m_zNear, m_zNear, m_zFar );
                Matrix4f.frustum(2.f*(float)Math.tan(halfFovx)*m_zNear, 2.f*(float)Math.tan(halfFovy)*m_zNear, m_zNear, m_zFar, virtualCameraProj);
            }
//        D3DXMatrixPerspectiveFovLH( &virtualCameraProj, 2.f*halfFovy, halfFovx/halfFovy, m_zNear, m_zFar);
        }
        else
        {
            /*D3DXMatrixIdentity( &virtualCameraView );
            D3DXMatrixPerspectiveFovLH( &virtualCameraProj, D3DXToRadian(60.f), m_fAspect, m_zNear, m_zFar);*/
            virtualCameraView.setIdentity();
            Matrix4f.perspective(60.f, sceneData.aspect, m_zNear, m_zFar, virtualCameraProj);
        }

        /*D3DXMatrixMultiply(&virtualCameraViewProj, &m_View, &virtualCameraView);
        D3DXMatrixMultiply(&virtualCameraViewProj, &virtualCameraViewProj, &virtualCameraProj);*/
        Matrix4f.mul(virtualCameraView, m_View, virtualCameraViewProj);
        Matrix4f.mul(virtualCameraProj, virtualCameraViewProj, virtualCameraViewProj);

        /*D3DXMATRIX eyeToPostProjectiveVirtualCamera;
        D3DXMatrixMultiply(&eyeToPostProjectiveVirtualCamera, &virtualCameraView, &virtualCameraProj);*/
        Matrix4f eyeToPostProjectiveVirtualCamera = Matrix4f.mul(virtualCameraProj, virtualCameraView, null);

//        D3DXVECTOR3 eyeLightDir;  D3DXVec3TransformNormal(&eyeLightDir, &m_lightDir, &m_View);
        Vector3f eyeLightDir = Matrix4f.transformNormal(m_View, m_lightDir, null);

        //  directional light becomes a point on infinity plane in post-projective space
        /*D3DXVECTOR4 lightDirW (eyeLightDir.x, eyeLightDir.y, eyeLightDir.z, 0.f);
        D3DXVECTOR4   ppLight;
        D3DXVec4Transform(&ppLight, &lightDirW, &virtualCameraProj);*/
        Vector4f ppLight = Matrix4f.transform(virtualCameraProj, new Vector4f(eyeLightDir.x, eyeLightDir.y, eyeLightDir.z, 0.f), null);

        boolean m_bShadowTestInverted = (ppLight.w < 0.f); // the light is coming from behind the eye

        //  compute the projection matrix...
        //  if the light is >= 1000 units away from the unit box, use an ortho matrix (standard shadow mapping)
        if ( (Math.abs(ppLight.w) <= W_EPSILON) )  // orthographic matrix; uniform shadow mapping
        {
            /*D3DXVECTOR3 ppLightDirection(ppLight.x, ppLight.y, ppLight.z);
            D3DXVec3Normalize(&ppLightDirection, &ppLightDirection);*/
            Vector3f ppLightDirection = new Vector3f(ppLight.x, ppLight.y, ppLight.z);
            ppLightDirection.normalise();

            /*BoundingBox ppUnitBox; ppUnitBox.maxPt = D3DXVECTOR3(1, 1, 1); ppUnitBox.minPt = D3DXVECTOR3(-1, -1, 0);
            D3DXVECTOR3 cubeCenter; ppUnitBox.Centroid( &cubeCenter );*/
            BoundingBox ppUnitBox = new BoundingBox(-1,-1, 0, 1,1,1);
            Vector3f cubeCenter = ppUnitBox.center(null);
            float[] t = new float[1];

            ppUnitBox.intersect( t, cubeCenter, ppLightDirection );
            /*D3DXVECTOR3 lightPos = cubeCenter + 2.f*t*ppLightDirection;*/
            Vector3f lightPos = Vector3f.linear(cubeCenter, ppLightDirection, 2.f*t[0], null);
            ReadableVector3f axis = yAxis;

            //  if the yAxis and the view direction are aligned, choose a different up vector, to avoid singularity
            //  artifacts
            if ( Math.abs(Vector3f.dot(ppLightDirection, yAxis))>0.99f )
            axis = zAxis;

            /*D3DXMatrixLookAtLH(&lightView, &lightPos, &cubeCenter, &axis);
            XFormBoundingBox(&ppUnitBox, &ppUnitBox, &lightView);
            D3DXMatrixOrthoOffCenterLH(&lightProj, ppUnitBox.minPt.x, ppUnitBox.maxPt.x, ppUnitBox.minPt.y, ppUnitBox.maxPt.y, ppUnitBox.minPt.z, ppUnitBox.maxPt.z);*/
            Matrix4f.lookAt(lightPos, cubeCenter, axis, lightView);
            BoundingBox.transform(lightView, ppUnitBox, ppUnitBox);
            Matrix4f.ortho(ppUnitBox._min.x, ppUnitBox._max.x, ppUnitBox._min.y, ppUnitBox._max.y, -ppUnitBox._max.z, -ppUnitBox._min.z, lightProj);

            m_ppNear = -ppUnitBox._max.z;
            m_ppFar  = -ppUnitBox._min.z;
            m_fSlideBack = 0.f;
        }
        else  // otherwise, use perspective shadow mapping
        {
            Vector3f ppLightPos = new Vector3f();
            float wRecip = 1.0f / ppLight.w;
            ppLightPos.x = ppLight.x * wRecip;
            ppLightPos.y = ppLight.y * wRecip;
            ppLightPos.z = ppLight.z * wRecip;

            Matrix4f eyeToPostProjectiveLightView;

            final float ppCubeRadius = 1.5f;  // the post-projective view box is [-1,-1,0]..[1,1,1] in DirectX, so its radius is 1.5
            final ReadableVector3f ppCubeCenter = new Vector3f(0.f, 0.f, 0.5f);

            if (m_bShadowTestInverted)  // use the inverse projection matrix
            {
                BoundingCone viewCone;
                if (!m_bUnitCubeClip)
                {
                    //  project the entire unit cube into the shadow map
                    ArrayList<BoundingBox> justOneBox = new ArrayList<>();
                    BoundingBox unitCube = new BoundingBox(-1.f, -1.f, 0.f,1.f, 1.f, 1.f );  // TODO Note the zmin value
                    /*unitCube.minPt = D3DXVECTOR3(-1.f, -1.f, 0.f);
                    unitCube.maxPt = D3DXVECTOR3( 1.f, 1.f, 1.f );*/
                    justOneBox.add(unitCube);
//                    D3DXMATRIX tmpIdentity;
//                    D3DXMatrixIdentity(&tmpIdentity);
                    viewCone = new BoundingCone(justOneBox, Matrix4f.IDENTITY, ppLightPos);
                }
                else
                {
                    //  clip the shadow map to just the used portions of the unit box.
                    viewCone = new BoundingCone(m_ShadowReceiverPoints, eyeToPostProjectiveVirtualCamera, ppLightPos);
                }

                //  construct the inverse projection matrix -- clamp the fNear value for sanity (clamping at too low
                //  a value causes significant underflow in a 24-bit depth buffer)
                //  the multiplication is necessary since I'm not checking shadow casters
                viewCone.fNear = Math.max(0.001f, viewCone.fNear*0.3f);
                /*m_ppNear = -viewCone.fNear;
                m_ppFar  = viewCone.fNear;*/
                m_ppNear = viewCone.fFar;  // TODO need check
                m_ppFar = -viewCone.fFar;
                lightView = viewCone.m_LookAt;
//                D3DXMatrixPerspectiveLH( &lightProj, 2.f*tanf(viewCone.fovx)*m_ppNear, 2.f*tanf(viewCone.fovy)*m_ppNear, m_ppNear, m_ppFar );
                Matrix4f.frustum(2.f*(float)Math.tan(viewCone.fovx)*m_ppNear, 2.f*(float)Math.tan(viewCone.fovy)*m_ppNear, m_ppNear, m_ppFar, lightProj);
                //D3DXMatrixPerspectiveFovLH(&lightProj, 2.f*viewCone.fovy, viewCone.fovx/viewCone.fovy, m_ppNear, m_ppFar);
            }
            else  // regular projection matrix
            {
                float fFovy, fAspect, fFar, fNear;
                if (!m_bUnitCubeClip)
                {
                    Vector3f lookAt = Vector3f.sub(ppCubeCenter,ppLightPos, null);

                    float distance = lookAt.length();
//                    lookAt = lookAt / distance;
                    lookAt.scale(1.f/distance);

                    ReadableVector3f axis = yAxis;
                    //  if the yAxis and the view direction are aligned, choose a different up vector, to avoid singularity
                    //  artifacts
                    if ( Math.abs(Vector3f.dot(yAxis, lookAt))>0.99f )
                    axis = zAxis;

                    //  this code is super-cheese.  treats the unit-box as a sphere
                    //  lots of problems, looks like hell, and requires that MinInfinityZ >= 2
//                    D3DXMatrixLookAtLH(&lightView, &ppLightPos, &ppCubeCenter, &axis);
                    Matrix4f.lookAt(ppLightPos, ppCubeCenter, axis, lightView);
                    fFovy = (float) (2.*Math.tan(ppCubeRadius/distance));
                    fAspect = 1.f;
                    fNear = Math.max(0.001f, distance - 2.f*ppCubeRadius);
                    fFar = distance + 2.f*ppCubeRadius;
                    BoundingBox ppView;
//                    D3DXMatrixMultiply(&eyeToPostProjectiveLightView, &eyeToPostProjectiveVirtualCamera, &lightView);
                    eyeToPostProjectiveLightView = Matrix4f.mul(lightView, eyeToPostProjectiveVirtualCamera, null);
                }
                else
                {
                    //  unit cube clipping
                    //  fit a cone to the bounding geometries of all shadow receivers (incl. terrain) in the scene
                    BoundingCone bc = new BoundingCone(m_ShadowReceiverPoints, eyeToPostProjectiveVirtualCamera, ppLightPos);
                    lightView = bc.m_LookAt;
//                    D3DXMatrixMultiply(&eyeToPostProjectiveLightView, &eyeToPostProjectiveVirtualCamera, &lightView);
                    eyeToPostProjectiveLightView = Matrix4f.mul(lightView, eyeToPostProjectiveVirtualCamera, null);
                    float fDistance = Vector3f.distance(ppLightPos,ppCubeCenter);
                    fFovy = 2.f * bc.fovy;
                    fAspect = bc.fovx / bc.fovy;
                    /*fFar = bc.fFar;
                    //  hack alert!  adjust the near-plane value a little bit, to avoid clamping problems
                    fNear = bc.fNear * 0.6f;*/
                    fFar = -bc.fNear;
                    fNear = -bc.fFar * 0.6f;
                }

                fNear = Math.max(0.001f, fNear);
                m_ppNear = fNear;
                m_ppFar = fFar;
//                D3DXMatrixPerspectiveFovLH(&lightProj, fFovy, fAspect, m_ppNear, m_ppFar);
                Matrix4f.perspective(fFovy, fAspect, m_ppNear, m_ppFar, lightProj);
            }
        }

        //  build the composite matrix that transforms from world space into post-projective light space
        /*D3DXMatrixMultiply(&m_LightViewProj, &lightView, &lightProj);
        D3DXMatrixMultiply(&m_LightViewProj, &virtualCameraViewProj, &m_LightViewProj);*/
        Matrix4f.mul(lightProj, lightView, m_LightViewProj);
        Matrix4f.mul(lightProj, virtualCameraViewProj, m_LightViewProj);
    }

    static BoundingBox wrap(List<Vector3f> points){
        BoundingBox result = new BoundingBox();
        for(Vector3f v : points){
            result.expandBy(v);
        }

        return result;
    }

    //-----------------------------------------------------------------------------
    // Name: BuildLiSPSMProjectionMatrix
    // Desc: Builds a light-space perspective shadow map projection matrix
    //       Much thanks to Oles Shishkovstov, who provided the original implementation
    //-----------------------------------------------------------------------------
    void BuildLSPSMProjectionMatrix(CameraData sceneData)
    {
        Matrix4f m_Projection = sceneData.projection;
        Matrix4f m_View = sceneData.getViewMatrix();
        if ( Math.abs(m_fCosGamma) >= 0.999f )  // degenerates to uniform shadow map
        {
            BuildOrthoShadowProjectionMatrix();
        }
        else
        {
            //  compute shadow casters & receivers
            ComputeVirtualCameraParameters(sceneData );

            /*std::vector<D3DXVECTOR3> bodyB; bodyB.reserve( m_ShadowCasterPoints.size()*8 + 8 );*/
            List<Vector3f> bodyB = new ArrayList<>(m_ShadowCasterPoints.size()*8 + 8);
            Frustum eyeFrustum = new Frustum( m_Projection );
            for ( int i=0; i<8; i++ ) bodyB.add( eyeFrustum.pntList[i] );

            //  build the convex body B by adding all the points for the shadow caster/receiver bounding boxes, plus
            //  the frustum extremities to a list of points
            /*std::vector<BoundingBox>::iterator boxIt = m_ShadowCasterPoints.begin();
            while ( boxIt != m_ShadowCasterPoints.end() )*/
            for(BoundingBox box :m_ShadowCasterPoints )
            {
//            const BoundingBox& box = *boxIt++;
                for ( int i=0; i<8; i++ ) bodyB.add( box.corner(i, null) );
            }

            //  compute the "light-space" basis, using the algorithm described in the paper
            //  note:  since bodyB is defined in eye space, all of these vectors should also be defined in eye space
           /* D3DXVECTOR3 leftVector, upVector, viewVector;
            const D3DXVECTOR3 eyeVector( 0.f, 0.f, -1.f );  // eye vector in eye space is always -Z
            D3DXVec3TransformNormal( &upVector, &m_lightDir, &m_View );  // lightDir is defined in eye space, so xform it
            //  note: lightDir points away from the scene, so it is already the "negative" up direction;
            //  no need to re-negate it.
            D3DXVec3Cross( &leftVector, &upVector, &eyeVector );
            D3DXVec3Normalize( &leftVector, &leftVector );
            D3DXVec3Cross( &viewVector, &upVector, &leftVector );
            D3DXMATRIX lightSpaceBasis;
            lightSpaceBasis._11 = leftVector.x; lightSpaceBasis._12 = upVector.x; lightSpaceBasis._13 = viewVector.x; lightSpaceBasis._14 = 0.f;
            lightSpaceBasis._21 = leftVector.y; lightSpaceBasis._22 = upVector.y; lightSpaceBasis._23 = viewVector.y; lightSpaceBasis._24 = 0.f;
            lightSpaceBasis._31 = leftVector.z; lightSpaceBasis._32 = upVector.z; lightSpaceBasis._33 = viewVector.z; lightSpaceBasis._34 = 0.f;
            lightSpaceBasis._41 = 0.f;          lightSpaceBasis._42 = 0.f;        lightSpaceBasis._43 = 0.f;          lightSpaceBasis._44 = 1.f;*/
            Vector3f leftVector = new Vector3f();
            Vector3f upVector = new Vector3f();
            Vector3f viewVector = new Vector3f();
            ReadableVector3f eyeVector = Vector3f.Z_AXIS_NEG;
            Matrix4f.transformNormal(m_View, m_lightDir, upVector);
            Vector3f.cross(upVector, eyeVector, leftVector);  leftVector.normalise();
            Vector3f.cross(upVector, leftVector, viewVector);
            Matrix4f lightSpaceBasis = new Matrix4f();
            lightSpaceBasis.m00 = leftVector.x; lightSpaceBasis.m10 = upVector.x; lightSpaceBasis.m20 = -viewVector.x; lightSpaceBasis.m30 = 0.f;
            lightSpaceBasis.m01 = leftVector.y; lightSpaceBasis.m11 = upVector.y; lightSpaceBasis.m21 = -viewVector.y; lightSpaceBasis.m31 = 0.f;
            lightSpaceBasis.m02 = leftVector.z; lightSpaceBasis.m12 = upVector.z; lightSpaceBasis.m22 = -viewVector.z; lightSpaceBasis.m32 = 0.f;
            lightSpaceBasis.m03 = 0.f;          lightSpaceBasis.m13 = 0.f;        lightSpaceBasis.m23 = 0.f;           lightSpaceBasis.m33 = 1.f;
            // TODO Above Code tranpose the lightSpaceBasis or nagative the viewVector.

            //  rotate all points into this new basis
//            D3DXVec3TransformCoordArray( &bodyB[0], sizeof(D3DXVECTOR3), &bodyB[0], sizeof(D3DXVECTOR3), &lightSpaceBasis, (UINT)bodyB.size() );
            for(int i = 0; i < bodyB.size(); i++){
                Matrix4f.transformNormal(lightSpaceBasis, bodyB.get(i), bodyB.get(i));
            }

            BoundingBox lightSpaceBox = wrap(bodyB);
            Vector3f lightSpaceOrigin = new Vector3f();
            //  for some reason, the paper recommended using the x coordinate of the xformed viewpoint as
            //  the x-origin for lightspace, but that doesn't seem to make sense...  instead, we'll take
            //  the x-midpt of body B (like for the Y axis)
            lightSpaceBox.center(lightSpaceOrigin );
            float sinGamma = (float) Math.sqrt( 1.f - m_fCosGamma*m_fCosGamma );

            //  use average of the "real" near/far distance and the optimized near/far distance to get a more pleasant result
            float Nopt0 = m_zNear + (float) Math.sqrt(m_zNear*m_zFar);
            float Nopt1 = ZNEAR_MIN + (float) Math.sqrt(ZNEAR_MIN*ZFAR_MAX);
            m_fLSPSM_Nopt  = (Nopt0 + Nopt1) / (2.f*sinGamma);
            //  add a constant bias, to guarantee some minimum distance between the projection point and the near plane
            m_fLSPSM_Nopt += 0.1f;
            //  now use the weighting to scale between 0.1 and the computed Nopt
            float Nopt = 0.1f + m_fLSPSM_NoptWeight * (m_fLSPSM_Nopt - 0.1f);

            lightSpaceOrigin.z = lightSpaceBox._min.z - Nopt; // TODO

            //  xlate all points in lsBodyB, to match the new lightspace origin, and compute the fov and aspect ratio
            float maxx=0.f, maxy=0.f, maxz=0.f;

            /*std::vector<D3DXVECTOR3>::iterator ptIt = bodyB.begin();
            while ( ptIt != bodyB.end() )*/
            for(Vector3f ptIt : bodyB)
            {
//                D3DXVECTOR3 tmp = *ptIt++ - lightSpaceOrigin;
                float tmpx = ptIt.x - lightSpaceOrigin.x;
                float tmpy = ptIt.y - lightSpaceOrigin.y;
                float tmpz = ptIt.z - lightSpaceOrigin.z;
                assert(tmpz > 0.f);
                maxx = Math.max(maxx, Math.abs(tmpx / tmpz));
                maxy = Math.max(maxy, Math.abs(tmpy / tmpz));
                maxz = Math.max(maxz, tmpz);
            }

            float fovy = atanf(maxy);
            float fovx = atanf(maxx);

            D3DXMATRIX lsTranslate, lsPerspective;

            D3DXMatrixTranslation(&lsTranslate, -lightSpaceOrigin.x, -lightSpaceOrigin.y, -lightSpaceOrigin.z);
            D3DXMatrixPerspectiveLH( &lsPerspective, 2.f*maxx*Nopt, 2.f*maxy*Nopt, Nopt, maxz );

            D3DXMatrixMultiply( &lightSpaceBasis, &lightSpaceBasis, &lsTranslate );
            D3DXMatrixMultiply( &lightSpaceBasis, &lightSpaceBasis, &lsPerspective );

            //  now rotate the entire post-projective cube, so that the shadow map is looking down the Y-axis
            D3DXMATRIX lsPermute, lsOrtho;

            lsPermute._11 = 1.f; lsPermute._12 = 0.f; lsPermute._13 = 0.f; lsPermute._14 = 0.f;
            lsPermute._21 = 0.f; lsPermute._22 = 0.f; lsPermute._23 =-1.f; lsPermute._24 = 0.f;
            lsPermute._31 = 0.f; lsPermute._32 = 1.f; lsPermute._33 = 0.f; lsPermute._34 = 0.f;
            lsPermute._41 = 0.f; lsPermute._42 = -0.5f; lsPermute._43 = 1.5f; lsPermute._44 = 1.f;

            D3DXMatrixOrthoLH( &lsOrtho, 2.f, 1.f, 0.5f, 2.5f );
            D3DXMatrixMultiply( &lsPermute, &lsPermute, &lsOrtho );
            D3DXMatrixMultiply( &lightSpaceBasis, &lightSpaceBasis, &lsPermute );

            if ( m_bUnitCubeClip )
            {
                std::vector<D3DXVECTOR3> receiverPts;
                std::vector<BoundingBox>::iterator rcvrIt = m_ShadowReceiverPoints.begin();
                receiverPts.reserve(m_ShadowReceiverPoints.size() * 8);
                while ( rcvrIt++ != m_ShadowReceiverPoints.end() )
                {
                    for ( int i=0; i<8; i++ )
                        receiverPts.push_back( rcvrIt->Point(i) );
                }

                D3DXVec3TransformCoordArray( &receiverPts[0], sizeof(D3DXVECTOR3), &receiverPts[0], sizeof(D3DXVECTOR3), &lightSpaceBasis, (UINT)receiverPts.size() );

                BoundingBox receiverBox( &receiverPts );
                receiverBox.maxPt.x = min( 1.f, receiverBox.maxPt.x );
                receiverBox.minPt.x = max(-1.f, receiverBox.minPt.x );
                receiverBox.maxPt.y = min( 1.f, receiverBox.maxPt.y );
                receiverBox.minPt.y = max(-1.f, receiverBox.minPt.y );
                float boxWidth = receiverBox.maxPt.x - receiverBox.minPt.x;
                float boxHeight = receiverBox.maxPt.y - receiverBox.minPt.y;

                if ( !ALMOST_ZERO(boxWidth) && !ALMOST_ZERO(boxHeight) )
                {
                    float boxX = ( receiverBox.maxPt.x + receiverBox.minPt.x ) * 0.5f;
                    float boxY = ( receiverBox.maxPt.y + receiverBox.minPt.y ) * 0.5f;

                    D3DXMATRIX clipMatrix( 2.f/boxWidth,  0.f, 0.f, 0.f,
                        0.f, 2.f/boxHeight, 0.f, 0.f,
                        0.f,           0.f, 1.f, 0.f,
                        -2.f*boxX/boxWidth, -2.f*boxY/boxHeight, 0.f, 1.f );
                    D3DXMatrixMultiply( &lightSpaceBasis, &lightSpaceBasis, &clipMatrix );
                }
            }

            D3DXMatrixMultiply( &m_LightViewProj, &m_View, &lightSpaceBasis );
        }
    }

//-----------------------------------------------------------------------------
// Name: BuildTSMProjectionMatrix
// Desc: Builds a trapezoidal shadow transformation matrix
//-----------------------------------------------------------------------------

    void BuildTSMProjectionMatrix()
    {
        //  this isn't strictly necessary for TSMs; however, my 'light space' matrix has a
        //  degeneracy when view==light, so this avoids the problem.
        if ( fabsf(m_fCosGamma) >= 0.999f )
        {
            BuildOrthoShadowProjectionMatrix();
        }
        else
        {
            //  update list of shadow casters/receivers
            ComputeVirtualCameraParameters();

            //  get the near and the far plane (points) in eye space.
            D3DXVECTOR3 frustumPnts[8];

            Frustum eyeFrustum( &m_Projection );  // autocomputes all the extrema points

            for ( int i=0; i<4; i++ )
            {
                frustumPnts[i]   = eyeFrustum.pntList[(i<<1)];       // far plane
                frustumPnts[i+4] = eyeFrustum.pntList[(i<<1) | 0x1]; // near plane
            }

            //   we need to transform the eye into the light's post-projective space.
            //   however, the sun is a directional light, so we first need to find an appropriate
            //   rotate/translate matrix, before constructing an ortho projection.
            //   this matrix is a variant of "light space" from LSPSMs, with the Y and Z axes permuted

            D3DXVECTOR3 leftVector, upVector, viewVector;
            const D3DXVECTOR3 eyeVector( 0.f, 0.f, -1.f );  //  eye is always -Z in eye space

            //  code copied straight from BuildLSPSMProjectionMatrix
            D3DXVec3TransformNormal( &upVector, &m_lightDir, &m_View );  // lightDir is defined in eye space, so xform it
            D3DXVec3Cross( &leftVector, &upVector, &eyeVector );
            D3DXVec3Normalize( &leftVector, &leftVector );
            D3DXVec3Cross( &viewVector, &upVector, &leftVector );

            D3DXMATRIX lightSpaceBasis;
            lightSpaceBasis._11 = leftVector.x; lightSpaceBasis._12 = viewVector.x; lightSpaceBasis._13 = -upVector.x; lightSpaceBasis._14 = 0.f;
            lightSpaceBasis._21 = leftVector.y; lightSpaceBasis._22 = viewVector.y; lightSpaceBasis._23 = -upVector.y; lightSpaceBasis._24 = 0.f;
            lightSpaceBasis._31 = leftVector.z; lightSpaceBasis._32 = viewVector.z; lightSpaceBasis._33 = -upVector.z; lightSpaceBasis._34 = 0.f;
            lightSpaceBasis._41 = 0.f;          lightSpaceBasis._42 = 0.f;          lightSpaceBasis._43 = 0.f;        lightSpaceBasis._44 = 1.f;

            //  rotate the view frustum into light space
            D3DXVec3TransformCoordArray( frustumPnts, sizeof(D3DXVECTOR3), frustumPnts, sizeof(D3DXVECTOR3), &lightSpaceBasis, sizeof(frustumPnts)/sizeof(D3DXVECTOR3) );

            //  build an off-center ortho projection that translates and scales the eye frustum's 3D AABB to the unit cube
            BoundingBox frustumBox( frustumPnts, sizeof(frustumPnts) / sizeof(D3DXVECTOR3) );

            //  also - transform the shadow caster bounding boxes into light projective space.  we want to translate along the Z axis so that
            //  all shadow casters are in front of the near plane.

            D3DXVECTOR3* shadowCasterPnts = NULL;
            shadowCasterPnts = new D3DXVECTOR3[8*m_ShadowCasterPoints.size()];
            for ( int i=0; i<m_ShadowCasterPoints.size(); i++ )
            {
                for ( int j=0; j<8; j++ ) shadowCasterPnts[i*8+j] = m_ShadowCasterPoints[i].Point(j);
            }

            D3DXVec3TransformCoordArray( shadowCasterPnts, sizeof(D3DXVECTOR3), shadowCasterPnts, sizeof(D3DXVECTOR3), &lightSpaceBasis, m_ShadowCasterPoints.size()*8 );
            BoundingBox casterBox( shadowCasterPnts, m_ShadowCasterPoints.size()*8 );
            delete [] shadowCasterPnts;

            float min_z = min( casterBox.minPt.z, frustumBox.minPt.z );
            float max_z = max( casterBox.maxPt.z, frustumBox.maxPt.z );

            if ( min_z <= 0.f )
            {
                D3DXMATRIX lightSpaceTranslate;
                D3DXMatrixTranslation( &lightSpaceTranslate, 0.f, 0.f, -min_z + 1.f );
                max_z = -min_z + max_z + 1.f;
                min_z = 1.f;
                D3DXMatrixMultiply ( &lightSpaceBasis, &lightSpaceBasis, &lightSpaceTranslate );
                D3DXVec3TransformCoordArray( frustumPnts, sizeof(D3DXVECTOR3), frustumPnts, sizeof(D3DXVECTOR3), &lightSpaceTranslate, sizeof(frustumPnts)/sizeof(D3DXVECTOR3) );
                frustumBox = BoundingBox( frustumPnts, sizeof(frustumPnts)/sizeof(D3DXVECTOR3) );
            }

            D3DXMATRIX lightSpaceOrtho;
            D3DXMatrixOrthoOffCenterLH( &lightSpaceOrtho, frustumBox.minPt.x, frustumBox.maxPt.x, frustumBox.minPt.y, frustumBox.maxPt.y, min_z, max_z );

            //  transform the view frustum by the new matrix
            D3DXVec3TransformCoordArray( frustumPnts, sizeof(D3DXVECTOR3), frustumPnts, sizeof(D3DXVECTOR3), &lightSpaceOrtho, sizeof(frustumPnts)/sizeof(D3DXVECTOR3) );


            D3DXVECTOR2 centerPts[2];
            //  near plane
            centerPts[0].x = 0.25f * (frustumPnts[4].x + frustumPnts[5].x + frustumPnts[6].x + frustumPnts[7].x);
            centerPts[0].y = 0.25f * (frustumPnts[4].y + frustumPnts[5].y + frustumPnts[6].y + frustumPnts[7].y);
            //  far plane
            centerPts[1].x = 0.25f * (frustumPnts[0].x + frustumPnts[1].x + frustumPnts[2].x + frustumPnts[3].x);
            centerPts[1].y = 0.25f * (frustumPnts[0].y + frustumPnts[1].y + frustumPnts[2].y + frustumPnts[3].y);

            D3DXVECTOR2 centerOrig = (centerPts[0] + centerPts[1])*0.5f;

            D3DXMATRIX trapezoid_space;

            D3DXMATRIX xlate_center(           1.f,           0.f, 0.f, 0.f,
                0.f,           1.f, 0.f, 0.f,
                0.f,           0.f, 1.f, 0.f,
                -centerOrig.x, -centerOrig.y, 0.f, 1.f );

            float half_center_len = D3DXVec2Length( &D3DXVECTOR2(centerPts[1] - centerOrig) );
            float x_len = centerPts[1].x - centerOrig.x;
            float y_len = centerPts[1].y - centerOrig.y;

            float cos_theta = x_len / half_center_len;
            float sin_theta = y_len / half_center_len;

            D3DXMATRIX rot_center( cos_theta, -sin_theta, 0.f, 0.f,
                sin_theta,  cos_theta, 0.f, 0.f,
                0.f,        0.f, 1.f, 0.f,
                0.f,        0.f, 0.f, 1.f );

            //  this matrix transforms the center line to y=0.
            //  since Top and Base are orthogonal to Center, we can skip computing the convex hull, and instead
            //  just find the view frustum X-axis extrema.  The most negative is Top, the most positive is Base
            //  Point Q (trapezoid projection point) will be a point on the y=0 line.
            D3DXMatrixMultiply( &trapezoid_space, &xlate_center, &rot_center );
            D3DXVec3TransformCoordArray( frustumPnts, sizeof(D3DXVECTOR3), frustumPnts, sizeof(D3DXVECTOR3), &trapezoid_space, sizeof(frustumPnts)/sizeof(D3DXVECTOR3) );

            BoundingBox frustumAABB2D( frustumPnts, sizeof(frustumPnts)/sizeof(D3DXVECTOR3) );

            float x_scale = max( fabsf(frustumAABB2D.maxPt.x), fabsf(frustumAABB2D.minPt.x) );
            float y_scale = max( fabsf(frustumAABB2D.maxPt.y), fabsf(frustumAABB2D.minPt.y) );
            x_scale = 1.f/x_scale;
            y_scale = 1.f/y_scale;

            //  maximize the area occupied by the bounding box
            D3DXMATRIX scale_center( x_scale, 0.f, 0.f, 0.f,
                0.f, y_scale, 0.f, 0.f,
                0.f,     0.f, 1.f, 0.f,
                0.f,     0.f, 0.f, 1.f );

            D3DXMatrixMultiply( &trapezoid_space, &trapezoid_space, &scale_center );

            //  scale the frustum AABB up by these amounts (keep all values in the same space)
            frustumAABB2D.minPt.x *= x_scale;
            frustumAABB2D.maxPt.x *= x_scale;
            frustumAABB2D.minPt.y *= y_scale;
            frustumAABB2D.maxPt.y *= y_scale;

            //  compute eta.
            float lambda = frustumAABB2D.maxPt.x - frustumAABB2D.minPt.x;
            float delta_proj = m_fTSM_Delta * lambda; //focusPt.x - frustumAABB2D.minPt.x;

        const float xi = -0.6f;  // 80% line

            float eta = (lambda*delta_proj*(1.f+xi)) / (lambda*(1.f-xi)-2.f*delta_proj);

            //  compute the projection point a distance eta from the top line.  this point is on the center line, y=0
            D3DXVECTOR2 projectionPtQ( frustumAABB2D.maxPt.x + eta, 0.f );

            //  find the maximum slope from the projection point to any point in the frustum.  this will be the
            //  projection field-of-view
            float max_slope = -1e32f;
            float min_slope =  1e32f;

            for ( int i=0; i < sizeof(frustumPnts)/sizeof(D3DXVECTOR3); i++ )
            {
                D3DXVECTOR2 tmp( frustumPnts[i].x*x_scale, frustumPnts[i].y*y_scale );
                float x_dist = tmp.x - projectionPtQ.x;
                if ( !(ALMOST_ZERO(tmp.y) || ALMOST_ZERO(x_dist)))
                {
                    max_slope = max(max_slope, tmp.y/x_dist);
                    min_slope = min(min_slope, tmp.y/x_dist);
                }
            }

            float xn = eta;
            float xf = lambda + eta;

            D3DXMATRIX ptQ_xlate(-1.f, 0.f, 0.f, 0.f,
                0.f, 1.f, 0.f, 0.f,
                0.f, 0.f, 1.f, 0.f,
                projectionPtQ.x, 0.f, 0.f, 1.f );
            D3DXMatrixMultiply( &trapezoid_space, &trapezoid_space, &ptQ_xlate );

            //  this shear balances the "trapezoid" around the y=0 axis (no change to the projection pt position)
            //  since we are redistributing the trapezoid, this affects the projection field of view (shear_amt)
            float shear_amt = (max_slope + fabsf(min_slope))*0.5f - max_slope;
            max_slope = max_slope + shear_amt;

            D3DXMATRIX trapezoid_shear( 1.f, shear_amt, 0.f, 0.f,
                0.f,       1.f, 0.f, 0.f,
                0.f,       0.f, 1.f, 0.f,
                0.f,       0.f, 0.f, 1.f );

            D3DXMatrixMultiply( &trapezoid_space, &trapezoid_space, &trapezoid_shear );


            float z_aspect = (frustumBox.maxPt.z-frustumBox.minPt.z) / (frustumAABB2D.maxPt.y-frustumAABB2D.minPt.y);

            //  perform a 2DH projection to 'unsqueeze' the top line.
            D3DXMATRIX trapezoid_projection(  xf/(xf-xn),          0.f, 0.f, 1.f,
                0.f, 1.f/max_slope, 0.f, 0.f,
                0.f,           0.f, 1.f/(z_aspect*max_slope), 0.f,
                -xn*xf/(xf-xn),           0.f, 0.f, 0.f );

            D3DXMatrixMultiply( &trapezoid_space, &trapezoid_space, &trapezoid_projection );

            //  the x axis is compressed to [0..1] as a result of the projection, so expand it to [-1,1]
            D3DXMATRIX biasedScaleX( 2.f, 0.f, 0.f, 0.f,
                0.f, 1.f, 0.f, 0.f,
                0.f, 0.f, 1.f, 0.f,
                -1.f, 0.f, 0.f, 1.f );
            D3DXMatrixMultiply( &trapezoid_space, &trapezoid_space, &biasedScaleX );

            D3DXMatrixMultiply( &trapezoid_space, &lightSpaceOrtho, &trapezoid_space );
            D3DXMatrixMultiply( &trapezoid_space, &lightSpaceBasis, &trapezoid_space );

            // now, focus on shadow receivers.
            if ( m_bUnitCubeClip )
            {
                D3DXVECTOR3* shadowReceiverPnts = NULL;
                shadowReceiverPnts = new D3DXVECTOR3[8*m_ShadowReceiverPoints.size()];
                for ( UINT i=0; i<m_ShadowReceiverPoints.size(); i++ )
                {
                    for ( int j=0; j<8; j++ ) shadowReceiverPnts[i*8+j] = m_ShadowReceiverPoints[i].Point(j);
                }

                D3DXVec3TransformCoordArray( shadowReceiverPnts, sizeof(D3DXVECTOR3), shadowReceiverPnts, sizeof(D3DXVECTOR3), &trapezoid_space, m_ShadowReceiverPoints.size()*8 );
                BoundingBox rcvrBox( shadowReceiverPnts, m_ShadowReceiverPoints.size()*8 );
                delete [] shadowReceiverPnts;
                //  never shrink the box, only expand it.
                rcvrBox.maxPt.x = min( 1.f, rcvrBox.maxPt.x );
                rcvrBox.minPt.x = max(-1.f, rcvrBox.minPt.x );
                rcvrBox.maxPt.y = min( 1.f, rcvrBox.maxPt.y );
                rcvrBox.minPt.y = max(-1.f, rcvrBox.minPt.y );
                float boxWidth  = rcvrBox.maxPt.x - rcvrBox.minPt.x;
                float boxHeight = rcvrBox.maxPt.y - rcvrBox.minPt.y;

                //  the receiver box is degenerate, this will generate specials (and there shouldn't be any shadows, anyway).
                if ( !(ALMOST_ZERO(boxWidth) || ALMOST_ZERO(boxHeight)) )
                {
                    //  the divide by two's cancel out in the translation, but included for clarity
                    float boxX = (rcvrBox.maxPt.x+rcvrBox.minPt.x) / 2.f;
                    float boxY = (rcvrBox.maxPt.y+rcvrBox.minPt.y) / 2.f;
                    D3DXMATRIX trapezoidUnitCube( 2.f/boxWidth,                 0.f, 0.f, 0.f,
                        0.f,       2.f/boxHeight, 0.f, 0.f,
                        0.f,                 0.f, 1.f, 0.f,
                        -2.f*boxX/boxWidth, -2.f*boxY/boxHeight, 0.f, 1.f );
                    D3DXMatrixMultiply( &trapezoid_space, &trapezoid_space, &trapezoidUnitCube );
                }
            }

            D3DXMatrixMultiply( &m_LightViewProj, &m_View, &trapezoid_space );

        }
    }


//-----------------------------------------------------------------------------
// Name: BuildOrthoShadowProjectionMatrix
// Desc: Builds an orthographic shadow transformation matrix
//-----------------------------------------------------------------------------

    void PracticalPSM::BuildOrthoShadowProjectionMatrix()
    {
        //  update the list of shadow casters and receivers.
        ComputeVirtualCameraParameters();

        D3DXMATRIX lightView, lightProj;
    const D3DXVECTOR3 zAxis(0.f, 0.f, 1.f);
    const D3DXVECTOR3 yAxis(0.f, 1.f, 0.f);
        D3DXVECTOR3 eyeLightDir;

        D3DXVec3TransformNormal(&eyeLightDir, &m_lightDir, &m_View);

        float fHeight = D3DXToRadian(60.f);
        float fWidth = m_fAspect * fHeight;

        BoundingBox frustumAABB;
        if ( m_bUnitCubeClip )
        {
            frustumAABB = BoundingBox( &m_ShadowReceiverPoints );
        }
        else
        {
            frustumAABB.minPt = D3DXVECTOR3(-fWidth*ZFAR_MAX, -fHeight*ZFAR_MAX, ZNEAR_MIN);
            frustumAABB.maxPt = D3DXVECTOR3( fWidth*ZFAR_MAX,  fHeight*ZFAR_MAX, ZFAR_MAX);
        }

        //  light pt is "infinitely" far away from the view frustum.
        //  however, all that's really needed is to place it just outside of all shadow casters

        BoundingBox casterAABB( &m_ShadowCasterPoints );
        D3DXVECTOR3 frustumCenter; frustumAABB.Centroid( &frustumCenter );
        float t;
        casterAABB.Intersect( &t, &frustumCenter, &eyeLightDir );

        D3DXVECTOR3 lightPt = frustumCenter + 2.f*t*eyeLightDir;
        D3DXVECTOR3 axis;

        if ( fabsf(D3DXVec3Dot(&eyeLightDir, &yAxis))>0.99f )
        axis = zAxis;
    else
        axis = yAxis;

        D3DXMatrixLookAtLH( &lightView, &lightPt, &frustumCenter, &axis );

        XFormBoundingBox( &frustumAABB, &frustumAABB, &lightView );
        XFormBoundingBox( &casterAABB,  &casterAABB,  &lightView );

        //  use a small fudge factor for the near plane, to avoid some minor clipping artifacts
        D3DXMatrixOrthoOffCenterLH( &lightProj, frustumAABB.minPt.x, frustumAABB.maxPt.x,
                frustumAABB.minPt.y, frustumAABB.maxPt.y,
                casterAABB.minPt.z, frustumAABB.maxPt.z );

        D3DXMatrixMultiply( &lightView, &m_View, &lightView );
        D3DXMatrixMultiply( &m_LightViewProj, &lightView, &lightProj );
    }
}