/*
 * Copyright (C) 2016 The Josh Tool Project
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

package com.mumu.pokemongogo.location;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PropertyService {
    private static String DIR = "/storage/emulated/0/";

    public static void setSystemProperty(String property, String value) {
        runCommandNoOutput("echo -n " + value + " > " + DIR + property);
    }

    public static String getSystemProperty(String property) {
        return runCommand("cat " + DIR + property);
    }

    /*
     * Run the specific command, you should not execute a command that will
     * cost more than 5 seconds.
     * TODO: Add timeout design for those time consumed commands
     */
    public static String runCommand(String cmdInput){
        String retStr = "";
        BufferedReader output;

        String[] cmd = {"/system/bin/sh", "-c", cmdInput};
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            output = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            retStr = output.readLine();
            if (retStr == null) {
                retStr = "";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return retStr;
    }

    /*
     * Run the specific command, you should not execute a command that will
     * cost more than 5 seconds.
     * This function will only execute with no output
     */
    public static void runCommandNoOutput(String cmdInput){
        String[] cmd = {"/system/bin/sh", "-c", cmdInput};
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
