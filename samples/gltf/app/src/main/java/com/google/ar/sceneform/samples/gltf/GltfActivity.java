/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.gltf;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class GltfActivity extends AppCompatActivity {
  private static final String TAG = GltfActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private ArFragment arFragment;
  private Renderable renderable;

  private static class AnimationInstance {
    Animator animator;
    Long startTime;
    float duration;
    int index;

    AnimationInstance(Animator animator, int index, Long startTime) {
      this.animator = animator;
      this.startTime = startTime;
      this.duration = animator.getAnimationDuration(index);
      this.index = index;
    }
  }

  private final Set<AnimationInstance> animators = new ArraySet<>();

  private final List<Color> colors =
      Arrays.asList(
          new Color(0, 0, 0, 1),
          new Color(1, 0, 0, 1),
          new Color(0, 1, 0, 1),
          new Color(0, 0, 1, 1),
          new Color(1, 1, 0, 1),
          new Color(0, 1, 1, 1),
          new Color(1, 0, 1, 1),
          new Color(1, 1, 1, 1));
  private int nextColor = 0;

  private ViewPager viewPager;
  private Adapter adapter;
  private List<Model> models = new ArrayList<>();
  private int models_position_selected = 0;
  private Integer[] colors_ = null;
  private ArgbEvaluator argbEvaluator = new ArgbEvaluator();


  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
      Log.d("teste", "teste");
      String url_ = "https://ar-api-zoo.herokuapp.com/armodels";
      RequestQueue requestQueue = Volley.newRequestQueue(this);
      JsonArrayRequest jsonObjectRequest  = new JsonArrayRequest(
              Request.Method.GET,
              url_,
              null,
              new Response.Listener<JSONArray>() {
                  @RequiresApi(api = VERSION_CODES.O)
                  @Override
                  public void onResponse(JSONArray response) {
                      for(int i = 0; i < response.length(); i++){
                          try {
                              JSONObject attachment = response.getJSONObject(i);
                              JSONObject image_object = attachment.getJSONObject("img");
                              JSONObject image_data_object = image_object.getJSONObject("data");
                              JSONArray jsonArrayOfBytes = image_data_object.getJSONArray(("data"));

                              byte[] bytes = new byte[jsonArrayOfBytes.length()];
                              for (int j = 0; j < jsonArrayOfBytes.length(); j++) {
                                  bytes[j]=(byte)(((int)jsonArrayOfBytes.get(j)) & 0xFF);
                              }
                              //String encoded = new String(Base64.getEncoder().encode(bytes)); //Bytes to base64
                              Bitmap decodedByte = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                              String title = attachment.getString("name");
                              String titleUpperFirstLetter = title.substring(0, 1).toUpperCase() + title.substring(1); // First letter upper case
                              String model_url = attachment.getString("modelUrl");

                              Model model_ = new Model(decodedByte, titleUpperFirstLetter, model_url);
                              models.add(model_);

                              adapter = new Adapter(models, GltfActivity.this);
                              viewPager = findViewById(R.id.viewPager);
                              viewPager.setAdapter(adapter);
                              //viewPager.setPadding(130, 0, 130,0);

                              Integer[] colors_temp = {
                                      getResources().getColor(R.color.cardview_dark_background),
                                      getResources().getColor(R.color.cardview_dark_background)
                              };
                              colors_ = colors_temp;
                              viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                                  @Override
                                  public void onPageScrolled(int i, float v, int i1) {
                                    /*if(i < (adapter.getCount() - 1) && i < (colors_.length - 1)){
                                        viewPager.setBackgroundColor((Integer) argbEvaluator.evaluate(i1, colors_[i], colors_[i + 1]));
                                    } else {
                                        viewPager.setBackgroundColor(colors_[colors_.length - 1]);
                                    }*/

                                  }

                                  @Override
                                  public void onPageSelected(int i) {
                                      Log.d("CHANGED::", i + "");
                                      Log.d("CHANGED::", models.get(i).getDesc());
                                      Log.d("CHANGED::", models.get(i).getTitle());
                                      models_position_selected = i;
                                      WeakReference<GltfActivity> weakActivity = new WeakReference<>(GltfActivity.this);
                                      ModelRenderable.builder()
                                              .setSource(GltfActivity.this, Uri.parse(models.get(i).getDesc()))
                                              .setIsFilamentGltf(true)
                                              .build()
                                              .thenAccept(
                                                      modelRenderable -> {
                                                          GltfActivity activity = weakActivity.get();
                                                          if (activity != null) {
                                                              activity.renderable = modelRenderable;
                                                          }
                                                      })
                                              .exceptionally(
                                                      throwable -> {
                                                          Toast toast =
                                                                  Toast.makeText(GltfActivity.this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
                                                          toast.setGravity(Gravity.CENTER, 0, 0);
                                                          toast.show();
                                                          return null;
                                                      });
                                  }

                                  @Override
                                  public void onPageScrollStateChanged(int i) {

                                  }
                              });
                          } catch (JSONException e) {
                              e.printStackTrace();
                              Log.d("JSONException", e.getMessage());
                          }
                      }

                    //Log.d("TESTE", response.toString());
                      //System.out.println(response.toString());
                  }
              }, new Response.ErrorListener() {
                  @Override
                  public void onErrorResponse(VolleyError error) {
                      System.out.println("EROORRRRRRRASDSA::" + error);
                  }
              }
      );
      requestQueue.add(jsonObjectRequest);

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    WeakReference<GltfActivity> weakActivity = new WeakReference<>(this);

    ModelRenderable.builder()
        .setSource(
            this,
            Uri.parse(
                //"https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
              //"https://storage.googleapis.com/ar-answers-in-search-models/static/aussie_animals/Koala.glb"))
            models.size() == 0 ? "https://storage.googleapis.com/ar-answers-in-search-models/static/aussie_animals/Koala.glb" : models.get(models_position_selected).getDesc()))

        .setIsFilamentGltf(true)
        .build()
        .thenAccept(
            modelRenderable -> {
              GltfActivity activity = weakActivity.get();
              if (activity != null) {
                activity.renderable = modelRenderable;
              }
            })
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (renderable == null) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable model and add it to the anchor.
          TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
          model.setParent(anchorNode);
          model.setRenderable(renderable);
          model.select();

          FilamentAsset filamentAsset = model.getRenderableInstance().getFilamentAsset();
          if (filamentAsset.getAnimator().getAnimationCount() > 0) {
            animators.add(new AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
          }

          Color color = colors.get(nextColor);
          nextColor++;
          for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
            Material material = renderable.getMaterial(i);
            material.setFloat4("baseColorFactor", color);
          }

          Node tigerTitleNode = new Node();
          tigerTitleNode.setParent(model);
          tigerTitleNode.setEnabled(false);
          tigerTitleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
          ViewRenderable.builder()
                  .setView(this, R.layout.tiger_card_view)
                  .build()
                  .thenAccept(
                          (renderable) -> {
                              tigerTitleNode.setRenderable(renderable);
                              tigerTitleNode.setEnabled(true);
                          })
                  .exceptionally(
                          (throwable) -> {
                              throw new AssertionError("Could not load card view.", throwable);
                          }
                  );
        });

    arFragment
        .getArSceneView()
        .getScene()
        .addOnUpdateListener(
            frameTime -> {
              Long time = System.nanoTime();
              for (AnimationInstance animator : animators) {
                animator.animator.applyAnimation(
                    animator.index,
                    (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                        % animator.duration);
                animator.animator.updateBoneMatrices();
              }
            });

  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }
}
