// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.crosswalkproject.ardrone;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramSocket;

import org.json.JSONException;
import org.json.JSONObject;

public class ARDrone extends XWalkExtensionClient {
    private static final String TAG = "ARDrone";

    private ATCommandManager mATCommandManager;
    private ATCommandQueue mQueue;
    private DatagramSocket mDataSocket;
    private String mLocalAddress;
    private String mRemoteAddress;
    private Thread mCommandThread;

    public ARDrone(String name, String jsApiContent, XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);
        mQueue = new ATCommandQueue(10);
        mLocalAddress = "192.168.1.2";
        mRemoteAddress = "192.168.1.1";
        mDataSocket = null;
    }

    private void handleMessage(int instanceID, String message) {
        try {
            JSONObject jsonInput = new JSONObject(message);
            String cmd = jsonInput.getString("cmd");
            String asyncCallId = jsonInput.getString("asyncCallId");
            handle(instanceID, asyncCallId, cmd);
        } catch (JSONException e) {
            printErrorMessage(e);
        }
    }

    private void handle(int instanceID, String asyncCallId, String cmd) {
        try {
            JSONObject jsonOutput = new JSONObject();

            if (cmd.equals("connect")) {
                jsonOutput.put("data", connect());
            } else if (cmd.equals("quit")) {
                jsonOutput.put("data", quit());
            } else if (cmd.equals("ftrim")) {
                jsonOutput.put("data", ftrim());
            } else if (cmd.equals("takeoff")) {
                jsonOutput.put("data", takeoff());
            } else if (cmd.equals("landing")) {
                jsonOutput.put("data", landing());
            } else if (cmd.equals("hover")) {
                jsonOutput.put("data", hover());
            } else if (cmd.equals("pitch_plus")) {
                jsonOutput.put("data", pitch_plus());
            } else if (cmd.equals("pitch_minus")) {
                jsonOutput.put("data", pitch_minus());
            } else if (cmd.equals("roll_plus")) {
                jsonOutput.put("data", roll_plus());
            } else if (cmd.equals("roll_minus")) {
                jsonOutput.put("data", roll_minus());
            } else if (cmd.equals("yaw_plus")) {
                jsonOutput.put("data", yaw_plus());
            } else if (cmd.equals("yaw_minus")) {
                jsonOutput.put("data", yaw_minus());
            }

            jsonOutput.put("asyncCallId", asyncCallId);
            postMessage(instanceID, jsonOutput.toString());
        } catch (JSONException e) {
            printErrorMessage(e);
        }
    }

    private JSONObject connect() {
        try {
            mDataSocket = new DatagramSocket();
            mATCommandManager = new ATCommandManager(mQueue, mDataSocket, mRemoteAddress);
            mCommandThread = new Thread(mATCommandManager);
            mCommandThread.start();
        } catch (IOException e) {
            return setErrorMessage(e.toString());
        }

        return setOneJSONObject("connect", "true");
    }

    private JSONObject quit() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new QuitCommand()));
        mDataSocket.close();
        mDataSocket = null;

        return setOneJSONObject("quit", "true");
    }

    private JSONObject ftrim() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new FtrimCommand()));

        return setOneJSONObject("ftrim", "true");
    }

    private JSONObject takeoff() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new TakeoffCommand()));

        return setOneJSONObject("takeoff", "true");
    }

    private JSONObject landing() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new LandingCommand()));
        
        return setOneJSONObject("landing", "true");
    }

    private JSONObject hover() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new HoverCommand()));

        return setOneJSONObject("hover", "true");
    }

    private JSONObject pitch_plus() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new MoveCommand(false, 0.1f, 0f, 0f, 0f)));

        return setOneJSONObject("pitch_plus", "true");
    }

    private JSONObject pitch_minus() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new MoveCommand(false, -0.1f, 0f, 0f, 0f)));

        return setOneJSONObject("pitch_minus", "true");
    }

    private JSONObject roll_plus() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new MoveCommand(false, 0f, 0.1f, 0f, 0f)));

        return setOneJSONObject("roll_plus", "true");
    }

    private JSONObject roll_minus() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new MoveCommand(false, 0f, -0.1f, 0f, 0f)));

        return setOneJSONObject("roll_minus", "true");
    }

    private JSONObject yaw_plus() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new MoveCommand(false, 0f, 0f, 0f, 0.1f)));

        return setOneJSONObject("yaw_plus", "true");
    }

    private JSONObject yaw_minus() {
        if (mDataSocket == null) {
            return setOneJSONObject("status", "not connected");
        }

        mQueue.add(new ATCommand(new MoveCommand(false, 0f, 0f, 0f, -0.1f)));

        return setOneJSONObject("yaw_minus", "true");
    }

    protected JSONObject setOneJSONObject(String key, String value) {
        JSONObject out = new JSONObject();
        try {
            out.put(key, value);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return out;
    }

    protected void printErrorMessage(JSONException e) {
        Log.e(TAG, e.toString());
    }

    protected JSONObject setErrorMessage(String error) {
        JSONObject out = new JSONObject();
        JSONObject errorMessage = new JSONObject();
        try {
            errorMessage.put("message", error);
            out.put("error", errorMessage);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return out;
    }

    @Override
    public void onResume() {
        connect();
    }

    @Override
    public void onPause() {
        quit();
    }

    @Override
    public void onDestroy() {
        quit();
    }

    @Override
    public void onMessage(int instanceID, String message) {
        if (!message.isEmpty()) {
            handleMessage(instanceID, message);
        }
    }

    @Override
    public String onSyncMessage(int instanceID, String message) {
        return null;
    }
}
