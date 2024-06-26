package me.cortex.voxy.client.core.rendering.post;

import com.gtnewhorizons.angelica.compat.toremove.MatrixStack;
import me.cortex.voxy.client.core.gl.GlFramebuffer;
import me.cortex.voxy.client.core.gl.GlTexture;
import me.cortex.voxy.client.core.gl.shader.Shader;
import me.cortex.voxy.client.core.gl.shader.ShaderType;
import me.cortex.voxy.client.core.rendering.util.GlStateCapture;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;

import static org.lwjgl.opengl.ARBComputeShader.glDispatchCompute;
import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.ARBShaderImageLoadStore.glBindImageTexture;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glGetUniformfv;
import static org.lwjgl.opengl.GL43.GL_DEPTH_STENCIL_TEXTURE_MODE;
import static org.lwjgl.opengl.GL44C.glBindImageTextures;
import static org.lwjgl.opengl.GL45C.glBlitNamedFramebuffer;
import static org.lwjgl.opengl.GL45C.glTextureParameterf;

public class PostProcessing {
    private final GlFramebuffer framebuffer;
    private final GlFramebuffer framebufferSSAO;
    private int width;
    private int height;
    private GlTexture colour;
    private GlTexture colourSSAO;
    private GlTexture depthStencil;
    private boolean didSSAO;
    private final FullscreenBlit emptyBlit = new FullscreenBlit("voxy:post/noop.frag");
    //private final FullscreenBlit blitTexture = new FullscreenBlit("voxy:post/blit_texture_cutout.frag");
    private final FullscreenBlit blitTexture = new FullscreenBlit("voxy:post/blit_texture_depth_cutout.frag");
    private final Shader ssaoComp = Shader.make()
            .add(ShaderType.COMPUTE, "voxy:post/ssao.comp")
            .compile();
    private final GlStateCapture glStateCapture = GlStateCapture.make()
            .addCapability(GL_STENCIL_TEST)
            .addCapability(GL_DEPTH_TEST)
            .addTexture(GL_TEXTURE0)
            .addTexture(GL_TEXTURE1)
            .addTexture(GL_TEXTURE2)
            .build();

    public PostProcessing() {
        this.framebuffer = new GlFramebuffer();
        this.framebufferSSAO = new GlFramebuffer();
    }

    public void setSize(int width, int height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            if (this.colour != null) {
                if (this.colourSSAO != null) {
                    this.colourSSAO.free();
                }
                this.colour.free();
                this.depthStencil.free();
            }

            this.colour = new GlTexture().store(GL_RGBA8, 1, width, height);
            this.colourSSAO = new GlTexture().store(GL_RGBA8, 1, width, height);
            this.depthStencil = new GlTexture().store(GL_DEPTH24_STENCIL8, 1, width, height);

            glTextureParameterf(this.colour.id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameterf(this.colour.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTextureParameterf(this.colourSSAO.id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameterf(this.colourSSAO.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            //glTextureParameterf(this.depthStencil.id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            //glTextureParameterf(this.depthStencil.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            this.framebuffer.bind(GL_COLOR_ATTACHMENT0, this.colour);
            this.framebuffer.bind(GL_DEPTH_STENCIL_ATTACHMENT, this.depthStencil);
            this.framebuffer.verify();

            this.framebufferSSAO.bind(GL_COLOR_ATTACHMENT0, this.colourSSAO);
            this.framebufferSSAO.bind(GL_DEPTH_STENCIL_ATTACHMENT, this.depthStencil);
            this.framebufferSSAO.verify();
        }
    }



    public void shutdown() {
        this.framebuffer.free();
        this.framebufferSSAO.free();
        if (this.colourSSAO != null) this.colourSSAO.free();
        if (this.colour != null) this.colour.free();
        if (this.depthStencil != null) this.depthStencil.free();
        this.emptyBlit.delete();
        this.blitTexture.delete();
        this.ssaoComp.free();
    }

    public void setup(int width, int height, int sourceFB) {
        this.didSSAO = false;
        this.glStateCapture.capture();

        this.setSize(width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer.id);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        glBlitNamedFramebuffer(sourceFB, this.framebuffer.id, 0,0, width, height, 0,0, width, height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

        //Create a stencil mask of terrain generated by minecraft
        glEnable(GL_STENCIL_TEST);
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glStencilFunc(GL_ALWAYS, 1, 0xFF);
        glStencilMask(0xFF);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);
        glColorMask(false,false,false,false);
        this.emptyBlit.blit();
        glColorMask(true,true,true,true);
        glDepthMask(true);
        glDisable(GL_DEPTH_TEST);

        //Clear the depth buffer we copied cause else it will interfear with results (not really i think idk)
        glClear(GL_DEPTH_BUFFER_BIT);


        //Make voxy terrain render only where there isnt mc terrain
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        glStencilFunc(GL_EQUAL, 1, 0xFF);


        //TODO: need to figure out how to do translucency cause doing it normally will cause water to have double translucency (i think)
    }

    //Computes ssao on the current framebuffer data and updates it
    // this means that translucency wont be effected etc
    public void computeSSAO(Matrix4f projection, MatrixStack stack) {
        this.didSSAO = true;

        this.ssaoComp.bind();
        float[] data = new float[4*4];
        var mat = new Matrix4f(projection).mul(stack.peek().getModel());
        mat.get(data);
        glUniformMatrix4fv(3, false, data);//MVP
        mat.invert();
        mat.get(data);
        glUniformMatrix4fv(4, false, data);//invMVP

        glBindImageTexture(0, this.colourSSAO.id, 0, false,0, GL_READ_WRITE, GL_RGBA8);
        glActiveTexture(GL_TEXTURE1);
        GL11C.glBindTexture(GL_TEXTURE_2D, this.depthStencil.id);
        glTexParameteri (GL_TEXTURE_2D, GL_DEPTH_STENCIL_TEXTURE_MODE, GL_DEPTH_COMPONENT);
        glActiveTexture(GL_TEXTURE2);
        GL11C.glBindTexture(GL_TEXTURE_2D, this.colour.id);

        glDispatchCompute((this.width+31)/32, (this.height+31)/32, 1);

        glBindFramebuffer(GL_FRAMEBUFFER, this.framebufferSSAO.id);
    }


    //Executes the post processing and emits to whatever framebuffer is currently bound via a blit
    public void renderPost(Matrix4f fromProjection, Matrix4f tooProjection, int outputFB) {
        glDisable(GL_STENCIL_TEST);



        glBindFramebuffer(GL_FRAMEBUFFER, outputFB);

        //This will need to be replaced with a blit shader to raster with respect to the stencil
        //glBlitNamedFramebuffer(this.framebuffer.id, outputFB, 0,0, this.width, this.height, 0,0, this.width, this.height, GL_COLOR_BUFFER_BIT, GL_NEAREST);


        this.blitTexture.bind();

        float[] data = new float[4*4];
        var mat = new Matrix4f(fromProjection).invert();
        mat.get(data);
        glUniformMatrix4fv(2, false, data);//inverse fromProjection
        tooProjection.get(data);
        glUniformMatrix4fv(3, false, data);//tooProjection


        glActiveTexture(GL_TEXTURE1);
        GL11C.glBindTexture(GL_TEXTURE_2D, this.depthStencil.id);
        glTexParameteri (GL_TEXTURE_2D, GL_DEPTH_STENCIL_TEXTURE_MODE, GL_DEPTH_COMPONENT);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, this.didSSAO?this.colourSSAO.id:this.colour.id);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        this.blitTexture.blit();
        glDisable(GL_DEPTH_TEST);
        glDepthMask(true);

        this.glStateCapture.restore();
    }
}
