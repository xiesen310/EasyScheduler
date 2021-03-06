/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.escheduler.server.worker.task;

import cn.escheduler.common.Constants;
import cn.escheduler.common.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * python command executor
 */
public class PythonCommandExecutor extends AbstractCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonCommandExecutor.class);

    public static final String PYTHON = "python";



    public PythonCommandExecutor(Consumer<List<String>> logHandler,
                                 String taskDir, String taskAppId, String tenantCode, String envFile,
                                 Date startTime, int timeout, Logger logger) {
        super(logHandler,taskDir,taskAppId, tenantCode, envFile, startTime, timeout, logger);
    }


    /**
     * build command file path
     *
     * @return
     */
    @Override
    protected String buildCommandFilePath() {
        return String.format("%s/py_%s.command", taskDir, taskAppId);
    }

    /**
     * create command file if not exists
     *
     * @param commandFile
     * @throws IOException
     */
    @Override
    protected void createCommandFileIfNotExists(String execCommand, String commandFile) throws IOException {
        logger.info("tenant :{}, work dir:{}", tenantCode, taskDir);

        if (!Files.exists(Paths.get(commandFile))) {
            logger.info("generate command file:{}", commandFile);

            StringBuilder sb = new StringBuilder(200);
            sb.append("#-*- encoding=utf8 -*-\n");

            sb.append("\n\n");
            sb.append(String.format("import py_%s_node\n",taskAppId));
            logger.info(sb.toString());

            // write data to file
            FileUtils.writeStringToFile(new File(commandFile), sb.toString(), StandardCharsets.UTF_8);
        }
    }

    @Override
    protected String commandType() {

        String envPath = System.getProperty("user.dir") + Constants.SINGLE_SLASH + "conf"+
                Constants.SINGLE_SLASH +"env" + Constants.SINGLE_SLASH + Constants.ESCHEDULER_ENV_SH;
        String pythonHome = getPythonHome(envPath);
        if (StringUtils.isEmpty(pythonHome)){
            return PYTHON;
        }
        return pythonHome;
    }

    @Override
    protected boolean checkShowLog(String line) {
        return true;
    }

    @Override
    protected boolean checkFindApp(String line) {
        return true;
    }


    /**
     *  get python home
     * @param envPath
     * @return
     */
    private static String getPythonHome(String envPath){
        BufferedReader br = null;
        String line = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(envPath)));
            while ((line = br.readLine()) != null){
                if (line.contains(Constants.PYTHON_HOME)){
                    sb.append(line);
                    break;
                }
            }
            String result = sb.toString();
            if (org.apache.commons.lang.StringUtils.isEmpty(result)){
                return null;
            }
            String[] arrs = result.split("=");
            if (arrs.length == 2){
                return arrs[1];
            }

        }catch (IOException e){
            logger.error("read file failed : " + e.getMessage(),e);
        }finally {
            try {
                if (br != null){
                    br.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
        }
        return null;
    }

}
