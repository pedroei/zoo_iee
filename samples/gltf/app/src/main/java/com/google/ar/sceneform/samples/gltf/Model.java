package com.google.ar.sceneform.samples.gltf;

import android.graphics.Bitmap;

public class Model {
    private Bitmap image;
    private String title;
    private String desc;

    public Model(Bitmap image, String title, String desc) {
        this.image = image;
        this.title = title;
        this.desc = desc;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
