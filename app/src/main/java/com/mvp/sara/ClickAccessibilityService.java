package com.mvp.sara;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;

import com.mvp.sara.handlers.ClickLabelHandler;
import com.mvp.sara.handlers.OpenCameraHandler;
import com.mvp.sara.handlers.TypeTextHandler;
import com.mvp.sara.handlers.WhatsAppHandler;
import com.mvp.sara.handlers.ReadScreenHandler;

import java.util.List;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.Deque;

public class ClickAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClickAccessibilitySvc";

    private final BroadcastReceiver clickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ClickLabelHandler.ACTION_CLICK_LABEL.equals(intent.getAction())) {
                String label = intent.getStringExtra(ClickLabelHandler.EXTRA_LABEL);
                Log.d(TAG, "Received broadcast with label: " + label);
                if (label != null && !label.isEmpty()) {
                    Log.d(TAG, "Received request for action: " + label);

                    if (label.equalsIgnoreCase("scroll")) {
                        performScroll();
                    } else if (label.equalsIgnoreCase("scroll_up")) {
                        performScrollUp();
                    } else if (label.equalsIgnoreCase("like")) {
                        Log.d(TAG, "About to perform double-tap for like");
                        performDoubleTapOnScreen();
                    } else if (label.equalsIgnoreCase("switch_camera")) {
                        Log.d(TAG, "About to switch camera");
                        switchCamera();
                    } else if (label.equalsIgnoreCase("next video")) {
                        Log.d(TAG, "About to swipe up for next video");
                        performSwipeUpToNextVideo();
                    } else if (label.equalsIgnoreCase("click on that video")) {
                        Log.d(TAG, "About to tap on current video");
                        performTapOnCurrentVideo();
                    }
                    else {
                        clickNodeWithText(label);
                    }
                } else {
                    Log.w(TAG, "Received broadcast but label was null or empty");
                }
            }
            else if ("com.mvp.sara.ACTION_PERFORM_BACK".equals(intent.getAction())) {
                Log.d(TAG, "Received ACTION_PERFORM_BACK broadcast");
                performBackAction();
            } else if ("com.mvp.sara.ACTION_ANSWER_CALL".equals(intent.getAction())) {
                Log.d(TAG, "About to answer call");
                answerCall();
            } else if ("com.mvp.sara.ACTION_REJECT_CALL".equals(intent.getAction())) {
                Log.d(TAG, "About to reject call");
                rejectCall();
            } else if (TypeTextHandler.ACTION_TYPE_TEXT.equals(intent.getAction())) {
                String text = intent.getStringExtra(TypeTextHandler.EXTRA_TEXT);
                Log.d(TAG, "Received TYPE_TEXT action: " + text);
                performTypeText(text);
            } else if (TypeTextHandler.ACTION_NEXT_LINE.equals(intent.getAction())) {
                Log.d(TAG, "Received NEXT_LINE action");
                performTypeText("\n");
            } else if (TypeTextHandler.ACTION_SELECT_ALL.equals(intent.getAction())) {
                Log.d(TAG, "Received SELECT_ALL action");
                performSelectAll();
            } else if (TypeTextHandler.ACTION_COPY.equals(intent.getAction())) {
                Log.d(TAG, "Received COPY action");
                performCopy();
            } else if (TypeTextHandler.ACTION_CUT.equals(intent.getAction())) {
                Log.d(TAG, "Received CUT action");
                performCut();
            } else if (TypeTextHandler.ACTION_PASTE.equals(intent.getAction())) {
                Log.d(TAG, "Received PASTE action");
                performPaste();
            } else if ("com.mvp.sara.ACTION_SEND_WHATSAPP".equals(intent.getAction())) {
                Log.d(TAG, "Received SEND_WHATSAPP action");
                String contactName = intent.getStringExtra("contact_name");
                String message = intent.getStringExtra("message");
                performWhatsAppSend(contactName, message);
            } else if (ClickLabelHandler.ACTION_TYPE_MUSIC_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(ClickLabelHandler.EXTRA_MUSIC_SEARCH);
                if (query != null) {
                    Log.d(TAG, "Received music search query: " + query);
                    // Start the Amazon Music automation flow
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        performAmazonMusicSearchFlow(query);
                    }, 2500); // Wait for app to open

                }
            }
        }
    };

    private BroadcastReceiver readScreenReceiver;
    private BroadcastReceiver clickPointReceiver;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "ClickAccessibilityService connected");
        
        // Configure the accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_CLICKED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT | 
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | 
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);

        // Register broadcast receiver for click commands and call actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(ClickLabelHandler.ACTION_CLICK_LABEL);
        filter.addAction("com.mvp.sara.ACTION_ANSWER_CALL");
        filter.addAction("com.mvp.sara.ACTION_REJECT_CALL");
        filter.addAction(TypeTextHandler.ACTION_TYPE_TEXT);
        filter.addAction(TypeTextHandler.ACTION_NEXT_LINE);
        filter.addAction(TypeTextHandler.ACTION_SELECT_ALL);
        filter.addAction(TypeTextHandler.ACTION_COPY);
        filter.addAction(TypeTextHandler.ACTION_CUT);
        filter.addAction(TypeTextHandler.ACTION_PASTE);
        filter.addAction("com.mvp.sara.ACTION_SEND_WHATSAPP");
        filter.addAction(ClickLabelHandler.ACTION_TYPE_MUSIC_SEARCH);
        registerReceiver(clickReceiver, filter, RECEIVER_EXPORTED);

        readScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ReadScreenHandler.ACTION_READ_SCREEN.equals(intent.getAction())) {
                    Log.d(TAG, "Read screen action received.");
                    readScreen();
                }
            }
        };
        IntentFilter readScreenFilter = new IntentFilter(ReadScreenHandler.ACTION_READ_SCREEN);
        registerReceiver(readScreenReceiver, readScreenFilter, RECEIVER_EXPORTED);

        // Register for custom click point
        clickPointReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.mvp.sara.ACTION_CLICK_POINT".equals(intent.getAction())) {
                    int x = intent.getIntExtra("x", -1);
                    int y = intent.getIntExtra("y", -1);
                    if (x != -1 && y != -1) {
                        performTapAtPosition(x, y);
                    }
                }
            }
        };
        IntentFilter clickPointFilter = new IntentFilter("com.mvp.sara.ACTION_CLICK_POINT");
        registerReceiver(clickPointReceiver, clickPointFilter, RECEIVER_EXPORTED);
    }

    private void clickNodeWithText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform click.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }

        // 1. Try exact text match
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        boolean clicked = tryClickNodes(nodes, text, "exact text");

        // 2. Try partial text match
        if (!clicked) {
            Log.d(TAG, "No exact match found for '" + text + "'. Trying partial text match.");
            clicked = findAndClickPartialText(rootNode, text.toLowerCase(Locale.ROOT));
        }

        // 3. Try content description match
        if (!clicked) {
            Log.d(TAG, "No partial text match. Trying content description match.");
            clicked = findAndClickByContentDescription(rootNode, text.toLowerCase(Locale.ROOT));
        }

        // 4. Try class name heuristics (e.g., Button)
        if (!clicked) {
            Log.d(TAG, "No content description match. Trying class name heuristics.");
            clicked = findAndClickByClassName(rootNode, "Button");
        }

        // 5. Log node tree for debugging
        if (!clicked) {
            Log.d(TAG, "No match found. Dumping node tree for debugging:");
            dumpNodeTree(rootNode, 0);
            FeedbackProvider.speakAndToast(this, "Couldn't click " + text);
        }

        rootNode.recycle();
    }

    private boolean tryClickNodes(List<AccessibilityNodeInfo> nodes, String text, String matchType) {
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null && node.isVisibleToUser()) {
                    AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
                    if (clickableNode != null) {
                        Log.d(TAG, "Clicking ancestor node for " + matchType + ": " + text);
                        clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        clickableNode.recycle();
                        node.recycle();
                        return true;
                    }
                }
                if (node != null) node.recycle();
            }
        }
        return false;
    }

    private boolean findAndClickPartialText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        boolean clicked = false;
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().toLowerCase(Locale.ROOT).contains(text)) {
            AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
            if (clickableNode != null) {
                Log.d(TAG, "Clicking ancestor node for partial text: " + text);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                clickableNode.recycle();
                clicked = true;
            }
        }
        for (int i = 0; i < node.getChildCount() && !clicked; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                clicked = findAndClickPartialText(child, text) || clicked;
                child.recycle();
            }
        }
        return clicked;
    }

    private boolean findAndClickByContentDescription(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        boolean clicked = false;
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.toString().toLowerCase(Locale.ROOT).contains(text)) {
            AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
            if (clickableNode != null) {
                Log.d(TAG, "Clicking ancestor node for content description: " + text);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                clickableNode.recycle();
                clicked = true;
            }
        }
        for (int i = 0; i < node.getChildCount() && !clicked; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                clicked = findAndClickByContentDescription(child, text) || clicked;
                child.recycle();
            }
        }
        return clicked;
    }

    private boolean findAndClickByClassName(AccessibilityNodeInfo node, String className) {
        if (node == null) return false;
        boolean clicked = false;
        if (node.getClassName() != null && node.getClassName().toString().contains(className)) {
            AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
            if (clickableNode != null) {
                Log.d(TAG, "Clicking ancestor node for class name: " + className);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                clickableNode.recycle();
                clicked = true;
            }
        }
        for (int i = 0; i < node.getChildCount() && !clicked; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                clicked = findAndClickByClassName(child, className) || clicked;
                child.recycle();
            }
        }
        return clicked;
    }

    private void dumpNodeTree(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("[").append(node.getClassName());
        if (node.getText() != null) sb.append(" text=\"").append(node.getText()).append("\"");
        if (node.getContentDescription() != null) sb.append(" desc=\"").append(node.getContentDescription()).append("\"");
        sb.append(" clickable=").append(node.isClickable());
        sb.append(" visible=").append(node.isVisibleToUser());
        sb.append("]");
        Log.d(TAG, sb.toString());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                dumpNodeTree(child, depth + 1);
                child.recycle();
            }
        }
    }

    private AccessibilityNodeInfo findClickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) {
                return current;
            }
            AccessibilityNodeInfo parent = current.getParent();
            if (current != node) current.recycle();
            current = parent;
        }
        return null;
    }

    private void performDoubleTapOnScreen() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int x = width / 2;
        int y = height / 2;

        Path tapPath = new Path();
        tapPath.moveTo(x, y);

        GestureDescription.StrokeDescription firstTap = new GestureDescription.StrokeDescription(tapPath, 0, 50);
        GestureDescription.StrokeDescription secondTap = new GestureDescription.StrokeDescription(tapPath, 150, 50);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(firstTap);
        builder.addStroke(secondTap);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Double-tap gesture completed.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Double tap performed");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Double-tap gesture cancelled.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Couldn't perform double tap");
            }
        }, null);
    }

    private void performScroll() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform scroll.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }
        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable != null) {
            Log.d(TAG, "Found scrollable node. Performing scroll forward.");
            boolean result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            scrollable.recycle();
            if (result) {
                FeedbackProvider.speakAndToast(this, "Scrolled down");
            } else {
                FeedbackProvider.speakAndToast(this, "Can't scroll down anymore");
            }
        } else {
            FeedbackProvider.speakAndToast(this, "No scrollable area found");
        }
        rootNode.recycle();
    }

    private void performScrollUp() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform scroll up.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }
        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable != null) {
            Log.d(TAG, "Found scrollable node. Performing scroll backward.");
            boolean result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            scrollable.recycle();
            if (result) {
                FeedbackProvider.speakAndToast(this, "Scrolled up");
            } else {
                FeedbackProvider.speakAndToast(this, "Can't scroll up anymore");
            }
        } else {
            FeedbackProvider.speakAndToast(this, "No scrollable area found");
        }
        rootNode.recycle();
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findScrollableNode(child);
            if (child != null) child.recycle();
            if (result != null) return result;
        }
        return null;
    }

    private void switchCamera() {
        Log.d(TAG, "switchCamera called");
        
        // First try to find the camera switch button using accessibility
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(TAG, "Root node found, searching for camera switch button");
            AccessibilityNodeInfo switchButton = findCameraSwitchButton(rootNode);
            if (switchButton != null) {
                Log.d(TAG, "Found camera switch button using accessibility, clicking it");
                switchButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                switchButton.recycle();
                rootNode.recycle();
                FeedbackProvider.speakAndToast(this, "Switching camera.");
                return;
            } else {
                Log.d(TAG, "No camera switch button found using accessibility");
            }
            rootNode.recycle();
        } else {
            Log.d(TAG, "Root node is null, cannot search for camera switch button");
        }
        
        // Fallback: use position-based method
        Log.d(TAG, "Camera switch button not found, using fallback position method");
        performFallbackCameraSwitch();
    }
    
    private void performFallbackCameraSwitch() {
        // Get screen dimensions to calculate relative position
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Based on the user's screenshot, the button is in the bottom-right corner,
        // in line with the shutter button.
        // X: To the right of the shutter button, around 88% of screen width.
        // Y: Inside the bottom control bar, around 93% of screen height.
        int x = (int) (screenWidth * 0.88f);
        int y = (int) (screenHeight * 0.93f);

        Log.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight);
        Log.d(TAG, "Tapping camera switch button at refined fallback position: (" + x + ", " + y + ")");

        performTapAtPosition(x, y);
        FeedbackProvider.speakAndToast(this, "Switching camera.");
    }
    
    private AccessibilityNodeInfo findCameraSwitchButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        // Log all clickable nodes for debugging
        if (node.isClickable()) {
            CharSequence text = node.getText();
            String resourceId = node.getViewIdResourceName();
            CharSequence contentDesc = node.getContentDescription();
            Log.d(TAG, "Found clickable node - Text: '" + text + "', ResourceID: '" + resourceId + "', ContentDesc: '" + contentDesc + "'");
        }
        
        // Try to find by text content (more specific patterns)
        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            Log.d(TAG, "Checking text: '" + text + "' (lowercase: '" + lowerText + "')");
            
            if (lowerText.contains("switch") || lowerText.contains("camera") || 
                lowerText.contains("flip") || lowerText.contains("front") || 
                lowerText.contains("back") || lowerText.contains("switch camera") ||
                lowerText.contains("camera switch") || lowerText.contains("flip camera") ||
                lowerText.contains("camera flip") || lowerText.equals("switch") ||
                lowerText.equals("flip") || lowerText.equals("camera")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found camera switch button by text: '" + text + "'");
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Try to find by resource ID (more specific patterns)
        String resourceId = node.getViewIdResourceName();
        if (resourceId != null) {
            String lowerResourceId = resourceId.toLowerCase();
            Log.d(TAG, "Checking resource ID: '" + resourceId + "' (lowercase: '" + lowerResourceId + "')");
            
            if (lowerResourceId.contains("switch") || 
                lowerResourceId.contains("camera") ||
                lowerResourceId.contains("flip") ||
                lowerResourceId.contains("switch_camera") ||
                lowerResourceId.contains("camera_switch") ||
                lowerResourceId.contains("flip_camera") ||
                lowerResourceId.contains("camera_flip") ||
                lowerResourceId.contains("switchcamera") ||
                lowerResourceId.contains("camerabutton") ||
                lowerResourceId.contains("flipbutton")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found camera switch button by resource ID: '" + resourceId + "'");
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Try to find by content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String lowerContentDesc = contentDesc.toString().toLowerCase();
            Log.d(TAG, "Checking content description: '" + contentDesc + "' (lowercase: '" + lowerContentDesc + "')");
            
            if (lowerContentDesc.contains("switch") || 
                lowerContentDesc.contains("camera") ||
                lowerContentDesc.contains("flip") ||
                lowerContentDesc.contains("switch camera") ||
                lowerContentDesc.contains("camera switch") ||
                lowerContentDesc.contains("flip camera") ||
                lowerContentDesc.contains("camera flip")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found camera switch button by content description: '" + contentDesc + "'");
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findCameraSwitchButton(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }
        
        return null;
    }

    private void performTapAtPosition(int x, int y) {
        Log.d(TAG, "performTapAtPosition called at (" + x + ", " + y + ")");
        
        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, 50);

        GestureDescription gestureDescription =
                new GestureDescription.Builder()
                        .addStroke(clickStroke)
                        .build();

        boolean result = dispatchGesture(gestureDescription, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Camera switch tap completed successfully");
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.e(TAG, "Camera switch tap was cancelled");
            }
        }, null);
        
        Log.d(TAG, "dispatchGesture result: " + result);
    }

    private void answerCall() {
        Log.d(TAG, "answerCall called");
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                telecomManager.acceptRingingCall();
                Log.d(TAG, "Called acceptRingingCall()");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in acceptRingingCall: " + e.getMessage());
                FeedbackProvider.speakAndToast(this, "Unable to answer call: permission denied.");
            }
        } else {
            Log.e(TAG, "TelecomManager is null");
            FeedbackProvider.speakAndToast(this, "Unable to answer call.");
        }
    }
    
    private void rejectCall() {
        Log.d(TAG, "rejectCall called");
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                telecomManager.endCall();
                Log.d(TAG, "Called endCall()");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in endCall: " + e.getMessage());
                FeedbackProvider.speakAndToast(this, "Unable to reject call: permission denied.");
            }
        } else {
            Log.e(TAG, "TelecomManager is null");
            FeedbackProvider.speakAndToast(this, "Unable to reject call.");
        }
    }

    private void performTypeText(String text) {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node == null) {
            // Try to focus the first editable field
            node = focusFirstEditableField();
        }
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing TYPE_TEXT: " + text);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            boolean setTextResult = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (!setTextResult) {
                // Fallback: try clipboard paste
                Log.d(TAG, "ACTION_SET_TEXT failed, trying clipboard paste fallback.");
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("label", text);
                clipboard.setPrimaryClip(clip);
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            node.recycle();
        } else {
            Log.w(TAG, "No editable text field focused for TYPE_TEXT");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private AccessibilityNodeInfo focusFirstEditableField() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo editable = findFirstEditText(root);
        if (editable != null) {
            editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            return editable;
        }
        if (root != null) root.recycle();
        return null;
    }

    private void performSelectAll() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing SELECT_ALL action");
            // Focus the node first, then try to select all
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            // For select all, we'll use a gesture to select all text
            // This is a simplified approach - in practice, you might need to use IME actions
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Select all performed");
        } else {
            Log.w(TAG, "No editable text field focused for SELECT_ALL");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performCopy() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing COPY action");
            // Focus the node and perform copy action
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Copy performed");
        } else {
            Log.w(TAG, "No editable text field focused for COPY");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performCut() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing CUT action");
            // Focus the node and perform cut action
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Cut performed");
        } else {
            Log.w(TAG, "No editable text field focused for CUT");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performPaste() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing PASTE action");
            // Focus the node and perform paste action
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Paste performed");
        } else {
            Log.w(TAG, "No editable text field focused for PASTE");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performWhatsAppSend(String contactName, String message) {
        Log.d(TAG, "Performing WhatsApp send for: " + contactName);
        
        // Wait a bit for WhatsApp to open and load
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Look for the send button in WhatsApp
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Try to find the send button by various methods
                AccessibilityNodeInfo sendButton = findWhatsAppSendButton(rootNode);
                if (sendButton != null) {
                    Log.d(TAG, "Found WhatsApp send button, clicking it");
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    sendButton.recycle();
                    
                    // Wait a bit more and then close WhatsApp
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        performBackAction();
                        FeedbackProvider.speakAndToast(this, "WhatsApp message sent to " + contactName);
                    }, 1000);
                } else {
                    Log.w(TAG, "Could not find WhatsApp send button, trying fallback method");
                    // Fallback: click 1cm to the right of the documents button
                    performFallbackWhatsAppSend(contactName);
                }
                rootNode.recycle();
            }
        }, 2000); // Wait 2 seconds for WhatsApp to load
    }
    
    private void performFallbackWhatsAppSend(String contactName) {
        Log.d(TAG, "Using fallback method to click send button");
        
        // Get screen dimensions
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        
        // Calculate position: send button is typically at the bottom-right area
        // Documents button is usually at the bottom, so we click 1cm to the right
        int clickX = screenWidth - 100; // 100px from right edge (approximately 1cm)
        int clickY = screenHeight - 150; // 150px from bottom edge
        
        Log.d(TAG, "Clicking at position: (" + clickX + ", " + clickY + ")");
        
        // Perform tap gesture at the calculated position
        performTapGesture(clickX, clickY);
        
        // Wait a bit more and then close WhatsApp
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            performBackAction();
            FeedbackProvider.speakAndToast(this, "WhatsApp message sent to " + contactName);
        }, 1000);
    }
    
    private AccessibilityNodeInfo findWhatsAppSendButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        // Try to find by text content (more specific WhatsApp send button texts)
        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            if (lowerText.equals("send") || lowerText.equals("enviar") || 
                lowerText.equals("→") || lowerText.equals("▶") || 
                lowerText.equals("send") || lowerText.equals("send") ||
                lowerText.contains("send") || lowerText.contains("enviar")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found send button by text: " + text);
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Try to find by resource ID (more specific WhatsApp send button IDs)
        String resourceId = node.getViewIdResourceName();
        if (resourceId != null) {
            String lowerResourceId = resourceId.toLowerCase();
            if (lowerResourceId.contains("send") || 
                lowerResourceId.contains("send_button") ||
                lowerResourceId.contains("sendbutton") ||
                lowerResourceId.contains("fab_send") ||
                lowerResourceId.contains("send_fab") ||
                lowerResourceId.contains("com.whatsapp:id/send")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found send button by resource ID: " + resourceId);
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Try to find by content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String lowerContentDesc = contentDesc.toString().toLowerCase();
            if (lowerContentDesc.contains("send") || 
                lowerContentDesc.contains("enviar") ||
                lowerContentDesc.contains("send message")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found send button by content description: " + contentDesc);
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findWhatsAppSendButton(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }
        
        return null;
    }
    
    private void performBackAction() {
        // Perform back action to close WhatsApp
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private AccessibilityNodeInfo getCurrentInputField() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        AccessibilityNodeInfo focused = findFocusedEditableNode(root);
        root.recycle();
        return focused;
    }

    private AccessibilityNodeInfo findFocusedEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isFocused() && node.isEditable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findFocusedEditableNode(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private void performAmazonMusicSearchFlow(String query) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot start Amazon Music search flow.");
            return;
        }
        // 1. Click the 'Find' button
        AccessibilityNodeInfo findButton = findNodeByDescOrText(rootNode, "find");
        if (findButton != null && findButton.isClickable()) {
            findButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            findButton.recycle();
            Log.d(TAG, "Clicked 'Find' button.");
            // 2. Wait, then click the search bar
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clickAmazonMusicSearchBarAndType(query);
            }, 1200);
        } else {
            Log.e(TAG, "Could not find 'Find' button in Amazon Music.");
        }
        rootNode.recycle();
    }

    private void clickAmazonMusicSearchBarAndType(String query) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo searchBar = findNodeByDescOrText(rootNode, "What do you want to hear?");
        if (searchBar != null && searchBar.isClickable()) {
            searchBar.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            searchBar.recycle();
            Log.d(TAG, "Clicked search bar.");
            // Wait, then type the query
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                typeInAmazonMusicSearch(query);
            }, 800);
        } else {
            Log.e(TAG, "Could not find search bar in Amazon Music.");
        }
        rootNode.recycle();
    }

    private void typeInAmazonMusicSearch(String query) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo editText = findFirstEditText(rootNode);
        if (editText != null) {
            editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query);
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            editText.recycle();
            Log.d(TAG, "Typed song name: " + query);
            // Wait, then press 'Done' (tick) on keyboard
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                pressKeyboardDoneButton();
            }, 800);
        } else {
            Log.e(TAG, "Could not find EditText to type in Amazon Music.");
        }
        rootNode.recycle();
    }

    private void pressKeyboardDoneButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo doneButton = findNodeByDescOrText(rootNode, "done");
        if (doneButton == null) {
            // Try tick unicode (✓)
            doneButton = findNodeByDescOrText(rootNode, "✓");
        }
        if (doneButton != null && doneButton.isClickable()) {
            doneButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            doneButton.recycle();
            Log.d(TAG, "Pressed 'Done' button on keyboard.");
            // Wait, then click the first song result
            new Handler(Looper.getMainLooper()).postDelayed(this::clickFirstAmazonMusicResult, 1200);
        } else {
            Log.e(TAG, "Could not find 'Done' button on keyboard.");
        }
        rootNode.recycle();
    }

    private void clickFirstAmazonMusicResult() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        // Try to find the first clickable node in the results list
        AccessibilityNodeInfo firstSong = findFirstClickableSongResult(rootNode);
        if (firstSong != null) {
            firstSong.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            firstSong.recycle();
            Log.d(TAG, "Clicked first song result in Amazon Music.");
        } else {
            Log.e(TAG, "Could not find first song result in Amazon Music.");
        }
        rootNode.recycle();
    }

    private AccessibilityNodeInfo findNodeByDescOrText(AccessibilityNodeInfo node, String keyword) {
        if (node == null) return null;
        String lowerKeyword = keyword.toLowerCase();
        if ((node.getContentDescription() != null && node.getContentDescription().toString().toLowerCase().contains(lowerKeyword)) ||
            (node.getText() != null && node.getText().toString().toLowerCase().contains(lowerKeyword))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByDescOrText(node.getChild(i), keyword);
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo findFirstEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if ("android.widget.EditText".equals(node.getClassName())) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findFirstEditText(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo findFirstClickableSongResult(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isClickable() && node.getClassName() != null && node.getClassName().toString().toLowerCase().contains("view")) {
            // Heuristic: clickable view in results
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findFirstClickableSongResult(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private void readScreen() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            FeedbackProvider.speakAndToast(this, "I can't access the screen right now.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        traverseNode(rootNode, sb);
        rootNode.recycle();

        if (sb.length() > 0) {
            FeedbackProvider.speakAndToast(this, sb.toString());
        } else {
            FeedbackProvider.speakAndToast(this, "I couldn't find any text to read on the screen.");
        }
    }

    private void traverseNode(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) {
            return;
        }

        if (node.getText() != null && !node.getText().toString().isEmpty()) {
            sb.append(node.getText().toString()).append(". ");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNode(node.getChild(i), sb);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (clickPointReceiver != null) unregisterReceiver(clickPointReceiver);
        if (readScreenReceiver != null) unregisterReceiver(readScreenReceiver);
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This can be left as is, or used for other purposes.
    }

    @Override
    public void onInterrupt() {
        Log.e("AccessService", "Service interrupted.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(clickReceiver);
    }

    // Intercept touch events to learn the switch button location
    @Override
    public boolean onGesture(int gestureId) {
        // Not used, but required to override
        return super.onGesture(gestureId);
    }

    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (OpenCameraHandler.isLearningSwitchButton() && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            OpenCameraHandler.setSwitchButtonCoordinates(x, y);
            FeedbackProvider.speakAndToast(this, "Switch button location saved.");
            return true;
        }
        return false;
    }

    private void performSwipeUpToNextVideo() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int x = width / 2;
        int startY = (int) (height * 0.75);
        int endY = (int) (height * 0.25);

        Path swipePath = new Path();
        swipePath.moveTo(x, startY);
        swipePath.lineTo(x, endY);

        GestureDescription.StrokeDescription swipe = new GestureDescription.StrokeDescription(swipePath, 0, 300);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(swipe);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Swipe up gesture completed.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Next video");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Swipe up gesture cancelled.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Couldn't go to next video");
            }
        }, null);
    }

    private void performTapOnCurrentVideo() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int x = width / 2;
        int y = height / 2;

        Path tapPath = new Path();
        tapPath.moveTo(x, y);

        GestureDescription.StrokeDescription tap = new GestureDescription.StrokeDescription(tapPath, 0, 50);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(tap);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Tap on video completed.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Clicked on video");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Tap on video cancelled.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Couldn't click on video");
            }
        }, null);
    }

    private void performTapGesture(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription tap = new GestureDescription.StrokeDescription(path, 0, 50);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(tap);
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Tap gesture completed at (" + x + ", " + y + ")");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Tap gesture cancelled at (" + x + ", " + y + ")");
            }
        }, null);
    }
} 