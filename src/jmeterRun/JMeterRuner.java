package jmeterRun;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.LoggerFactory;

import utils.APIReportProcessing;
import utils.ConfigReader;
import utils.Helper;
import utils.Logging;
import utils.SendMail;

public class JMeterRuner {

    public static void main(String[] args) {
        try {
            System.out.println("API functional test Started  ...");
            PropertyConfigurator.configure("Log4j.properties");
            Helper.logger = LoggerFactory.getLogger("API functional test report");
            Logging.log("info", "API functional test Started  ...");
            HashMap<String, String> globalConfig = new HashMap<String, String>();
            HashMap<String, String> jmeterExecConfig = new HashMap<String, String>();
            String jmeterExecutablePath = "";
            String resultFolder = "";
            String xslFileLocation = "";
            //
            String dir = System.getProperty("user.dir");
          //  dir=dir+"\\resources";
            System.out.println("dir="+dir);
            String resourcesDirPath = dir+"\\"+"resources";// System.getProperty("resources");
            String baseFolder = dir;//System.getProperty("BaseFolder");
            String jmxFolderPath = "";
            boolean mailerEnabled = false;
            try {
                mailerEnabled = Boolean.valueOf(System.getProperty("mailerEnabled"));
            } catch (Exception e) {
                mailerEnabled = false;
            }
            String receipientAddresses = System.getProperty("sendTo");
            if (!resourcesDirPath.isEmpty()) {
                globalConfig = new ConfigReader(resourcesDirPath +"\\" +"Configuration"+"\\"+"General.properties").getConfig();
                Helper.generateJmeterProperyFile(resourcesDirPath);
                jmeterExecConfig = new ConfigReader(resourcesDirPath+"\\" + "Configuration"+"\\"+"JmeterTestExecution.properties")
                        .getConfig();
                jmeterExecutablePath = Helper.checkForValidDirectory(resourcesDirPath +"\\"+ globalConfig.get("Jmeter_Path"));
                resultFolder = Helper.checkForValidDirectory(baseFolder+"\\" + globalConfig.get("Result_Path"));
                xslFileLocation = resourcesDirPath + "\\"+"Configuration"+"\\"+"Design.xsl";
                jmxFolderPath = resourcesDirPath+"\\" + "JMX_Modules"+"\\";
                Helper.createDir(resultFolder);
                Helper.loadResources(resourcesDirPath, resultFolder);
            } else {
                System.out.println("resources directory path is empty. Please check maven configuration.");
                Logging.log("error", "resources directory path is empty. Please check maven configuration.");
                return;
            }

            // Setting global runtime params...
            String reportDateTime = Helper.getCurrentDate();
            Set<String> APIs = jmeterExecConfig.keySet();
            ArrayList<String[]> moduleReport = new ArrayList<String[]>();
            for (String api : APIs) {
                String apiFile = "";
                apiFile = jmeterExecConfig.get(api);
                if (!apiFile.endsWith(".jmx"))
                    apiFile = apiFile + ".jmx";
                apiFile = jmxFolderPath + apiFile;
                File jmxFile = new File(apiFile);
                String[] moduleReportData = {"", "", ""};
                if (jmxFile.exists()) {
                    RunProcess runProcess = new RunProcess(jmeterExecutablePath, api, apiFile, resultFolder,
                            xslFileLocation, reportDateTime);
                    moduleReportData = runProcess.executeJMeterAndWriteResults();
                    moduleReport.add(moduleReportData);
                } else {
                    System.out.println(apiFile + " JMX file does not exist.");
                    Logging.log("info", apiFile + " JMX file does not exist.");
                }

                if (mailerEnabled) {
                    SendMail sendMail = new SendMail(receipientAddresses);
                    sendMail.sendMailNotification(baseFolder + globalConfig.get("Result_Path"), moduleReportData);
                }
            }
            if (moduleReport.size() > 0) {
                String successRate=globalConfig.get("TestStatusQualifier");
                if(successRate.isEmpty())
                    successRate="99";
//                APIReportProcessing.generateSummaryReportFile(resultFolder, moduleReport,successRate);
                APIReportProcessing.generateSummaryReportFile_New(resultFolder, moduleReport,successRate);
            }
            Helper.archiveReports(resultFolder, resultFolder + "archived_" + Helper.getCurrentDate() + ".zip");

            Logging.log("info", "Executed Jmeter Functional TestCase(s).");
        } catch (Exception e) {
            e.printStackTrace();
            Logging.log("error", "Some error occurred. Exception:" + e.getMessage());
        }
    }
}