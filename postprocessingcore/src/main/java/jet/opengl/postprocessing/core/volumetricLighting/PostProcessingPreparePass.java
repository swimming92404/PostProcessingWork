package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

final class PostProcessingPreparePass extends PostProcessingRenderPass{

    private SharedData m_sharedData;
    private LightScatteringInitAttribs m_InitAttribs;
    private LightScatteringFrameAttribs m_frameAttribs;

    public PostProcessingPreparePass(SharedData sharedData, LightScatteringInitAttribs initAttribs, LightScatteringFrameAttribs frameAttribs) {
        super("LightScattering Prepare");

        m_InitAttribs = initAttribs;
        m_frameAttribs = frameAttribs;

        m_sharedData = sharedData;

        // no inputs and no outputs.
        set(0, 0);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        m_sharedData.prepare(context.getFrameAttribs(), m_InitAttribs, m_frameAttribs);
    }

    @Override
    public void dispose() {
        m_sharedData.dispose();
    }
}
