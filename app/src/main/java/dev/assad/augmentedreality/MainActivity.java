package dev.assad.augmentedreality;

import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.google.ar.core.ArCoreApk;

import static com.google.ar.core.ArCoreApk.InstallStatus.INSTALLED;
import static com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED;


public class MainActivity extends AppCompatActivity {


    ArFragment fragment;
    private PointerDrawable pointer = new PointerDrawable();
    private boolean isTracking;
    private boolean isHitting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // AR
        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate();
        });
        initializeGallery();

    }

    private void onUpdate() {//Update the State of the AR Camera state and Track if there was any movements
        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }

    }

    private boolean updateTracking() { //Gets AR Camera State
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth()/2, vw.getHeight()/2);
    }


    private boolean mUserRequestedInstall = true;


    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }


    private void initializeGallery() {//adding Models and Thumbnails
        LinearLayout gallery = findViewById(R.id.gallery_layout);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(250,250);


        ImageView sofa = new ImageView(this);
        sofa.setImageResource(R.drawable.couch);
        sofa.setContentDescription("couch");
        sofa.setLayoutParams(layoutParams);
        sofa.setOnClickListener(view -> {
            addObject(Uri.parse("model.sfb"),0f);
        });

        ImageView redSofa = new ImageView(this);
        redSofa.setImageResource(R.drawable.redsofa);
        redSofa.setContentDescription("redSofa");
        redSofa.setLayoutParams(layoutParams);
        redSofa.setOnClickListener(view -> {
            addObject(Uri.parse("Sofa_01.sfb"),0f);
        });


        ImageView curtains = new ImageView(this);
        curtains.setImageResource(R.drawable.curtains);
        curtains.setContentDescription("Curtains");
        curtains.setLayoutParams(layoutParams);
        curtains.setOnClickListener(view -> {
            addObject(Uri.parse("502 Curtains.sfb"),0f);
        });
        /*ImageView minion = new ImageView(this);
        minion.setImageResource(R.drawable.minion);
        minion.setContentDescription("Minion");

        minion.setLayoutParams(layoutParams);
        minion.setOnClickListener(view -> {
            addObject(Uri.parse("tinker.sfb"));
        });*/

        //gallery.addView(minion);
        gallery.addView(sofa);
        gallery.addView(redSofa);
        gallery.addView(curtains);

    }
    private void addObject(Uri model,float orientation) {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    placeObject(fragment, hit.createAnchor(), model,orientation);
                    break;
                }
            }
        }
    }

    private void placeObject(ArFragment fragment, Anchor anchor, Uri model,float orientation) {
        CompletableFuture<Void> renderableFuture =
                ModelRenderable.builder()
                        .setSource(fragment.getContext(), model)
                        .build()
                        .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable,orientation))
                        .exceptionally((throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage())
                                    .setTitle("Could't build the Object!");
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return null;
                        }));
    }

    /*void checkObjectCollision()
    {
        if (fragment.getArSceneView().hasOverlappingRendering())
            Toast.makeText(this,"Collision Happened .. You have two object overlapping each other Please" +
                    " rearrange your objects to avoid collision",Toast.LENGTH_LONG).show();
    }*/


    List<Node> renderedObjects = new LinkedList<Node>();
    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable, float orientation) {

        //checkObjectCollision();
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), orientation));
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
        renderedObjects.add(node);
    }
}

