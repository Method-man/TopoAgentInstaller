package org.hkfree.topoagent.silentinstaller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.logging.Level;
import java.util.logging.Logger;
import jwrapper.jwutils.JWInstallApp;
import jwrapper.jwutils.JWSystem;

/**
 *
 * @author Filip Valenta
 */
public class TopoAgentSilentInstaller {

    public static final String DIRECTORY_RESOURCE = "winpcap";

    public static final String DIRECTORY_OS_64 = "x64";
    public static final String DIRECTORY_OS_32 = "x86";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            File appInstallFolder = JWSystem.getAppFolder().getParentFile();
            File[] filesInAppFodler = appInstallFolder.listFiles();

            String launcher = JWSystem.getAppBundleName() + "WinLauncher.exe";  // musime zvolit trosku heuristicky
            if (filesInAppFodler.length == 1) { // muzeme si byt jisti
                launcher = filesInAppFodler[0].getName();
            }
            
            JWInstallApp.addUninstallerShortcut("odinstalovat TopologyAgent");
            
            /**
             * nekdy cesta k souboru obsahuje mezery http://support.microsoft.com/kb/823093
             * dokumentace prikazu http://ss64.com/nt/schtasks.html
             */
            String createScheduledTask = "SCHTASKS /Create /tn \"TopoAgent\" /tr \"\\\""+appInstallFolder+"\\" + launcher + "\\\"\" /sc ONLOGON /RL HIGHEST /f";
            execCommand(createScheduledTask, "");
            
            System.out.println(createScheduledTask);
            // zpusob pres zastupce, nefunguje protoze app chce prava administratora
            // JWInstallApp.addAppShortcutInFolder("TopologyAgent", "TopologyAgent", new File("logo2.png"), 1, new File(getStartupFolder()));
            if (is64bitOS()) {
                // nahraje se to do SysWOW64
                String windir = System.getenv("WINDIR") + "\\System32";
                Files.copy(gfs(DIRECTORY_OS_64 + "/SysWOW64/Packet.dll"), gfp(windir + "\\Packet.dll"), REPLACE_EXISTING);
                Files.copy(gfs(DIRECTORY_OS_64 + "/SysWOW64/pthreadVC.dll"), gfp(windir + "\\pthreadVC.dll"), REPLACE_EXISTING);
                Files.copy(gfs(DIRECTORY_OS_64 + "/SysWOW64/wpcap.dll"), gfp(windir + "\\wpcap.dll"), REPLACE_EXISTING);

                // vynutit nahrat do System32
                windir = System.getenv("WINDIR") + "\\Sysnative";
                Files.copy(gfs(DIRECTORY_OS_64 + "/System32/Packet.dll"), gfp(windir + "\\Packet.dll"), REPLACE_EXISTING);
                Files.copy(gfs(DIRECTORY_OS_64 + "/System32/wpcap.dll"), gfp(windir + "\\wpcap.dll"), REPLACE_EXISTING);

                Files.copy(gfs(DIRECTORY_OS_64 + "/System32/drivers/npf.sys"), gfp(windir + "\\drivers\\npf.sys"), REPLACE_EXISTING);
            }
            else {
                String windir = System.getenv("WINDIR") + "\\System32";
                Files.copy(gfs(DIRECTORY_OS_32 + "/wpcap.dll"), gfp(windir + "\\wpcap.dll"), REPLACE_EXISTING);
                Files.copy(gfs(DIRECTORY_OS_32 + "/Packet.dll"), gfp(windir + "\\Packet.dll"), REPLACE_EXISTING);
                Files.copy(gfs(DIRECTORY_OS_32 + "/pthreadVC.dll"), gfp(windir + "\\pthreadVC.dll"), REPLACE_EXISTING);
                Files.copy(gfs(DIRECTORY_OS_32 + "/drivers/npf.sys"), gfp(windir + "\\drivers\\npf.sys"), REPLACE_EXISTING);
            }

        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

    }

    private static InputStream gfs(String file) {
        return (TopoAgentSilentInstaller.class.getResourceAsStream(DIRECTORY_RESOURCE + "/" + file));
    }

    private static Path gfp(String file) {
        return (new File(file)).toPath();
    }

    private static String getStartupFolder() {
        RegKeyManager rkm = new RegKeyManager();
        try {
            // rkm.query("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Common Startup");
            rkm.query("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Startup");
        } catch (Exception ex) {
            Logger.getLogger(TopoAgentSilentInstaller.class.getName()).log(Level.SEVERE, null, ex);
        }

        return rkm.getValue();
    }

    private static boolean is64bitOS() {
        boolean is64bit = false;
        if (System.getProperty("os.name").contains("Windows")) {
            is64bit = (System.getenv("ProgramFiles(x86)") != null);
        }
        else {
            is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
        }
        return is64bit;
    }

    private static void execCommand(String command, String notice) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            String line;

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (line.trim().equals("")) {
                    continue;
                }
                System.out.println(line.trim());
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
