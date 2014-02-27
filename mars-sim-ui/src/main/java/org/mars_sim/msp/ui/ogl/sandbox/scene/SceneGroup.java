package org.mars_sim.msp.ui.ogl.sandbox.scene;

import javax.media.opengl.GL2;

public class SceneGroup
extends RotatingObjectAbstract {

	public SceneGroup(
		double[] translation,
		double[] rotation,
		double[] deltaRotation,
		double[] scale
	) {
		super(translation,rotation,deltaRotation);
		this.setScale(scale);
	}

	@Override
	public void preinit(GL2 gl) {
		System.out.println("beginning opengl scene initialization.");
		super.preinit(gl);
	}

	@Override
	public void postinit(GL2 gl) {
		super.postinit(gl);
		System.out.println("finished opengl scene initialization.");
	}

	@Override
	public void prerender(GL2 gl) {
		super.prerender(gl);
		double[] skalo = this.getParamDoubleArray(PARAM_SCALE);
		gl.glScaled(skalo[0],skalo[1],skalo[2]);
	}
	
	public void setScale(double[] scale) {
		this.setParam(PARAM_SCALE,scale);
	}
}
