import java.nio.file.Path;
import java.nio.file.Paths;

public class SetXScriptTemplate {
    public static void main(String[] args) throws Exception {
        Path jsonDir = Paths.get("update");

        boolean changed = false;

        for (var channel : Channel.channels) {
            String url = "https://gitee.com/Glavo/HMCL-Update/raw/main/update/%s.json".formatted(channel.name());

            // https://stackoverflow.com/a/59571160/7659948
            String res = """
                    @echo off
                    SETLOCAL EnableDelayedExpansion
                    chcp 65001
                    set HMCL_UPDATE_ARG=-Dhmcl.update_source.override=
                    set HMCL_UPDATE_JSON=$$HMCL_UPDATE_JSON$$
                    set HMCL_UPDATE_JAVA_OPTION=%HMCL_UPDATE_ARG%%HMCL_UPDATE_JSON%
                    if "%JAVA_TOOL_OPTIONS%JUST_TEST"=="JUST_TEST" (
                        SetX JAVA_TOOL_OPTIONS %HMCL_UPDATE_JAVA_OPTION%
                    ) else (
                        for /f "usebackq tokens=2,*" %%A in (`reg query HKCU\\Environment /v JAVA_TOOL_OPTIONS`) do set _TEMP_JAVA_TOOL_OPTIONS=%%B
                        if "!_TEMP_JAVA_TOOL_OPTIONS!JUST_TEST"=="JUST_TEST" (
                            choice /c:yn /m "你已经在设置了系统变量 'JAVA_TOOL_OPTIONS'，所以无法修改环境变量。是否要打开系统属性窗口？(Y/N)"
                            if !errorlevel!==1 SystemPropertiesAdvanced
                        ) ELSE (
                            powershell -Command "SetX JAVA_TOOL_OPTIONS $(If ($env:_TEMP_JAVA_TOOL_OPTIONS -Match '(?<=\\s|^^)-Dhmcl\\.update_source\\.override=\\S*') {$env:_TEMP_JAVA_TOOL_OPTIONS -replace '(?<=\\s|^^)-Dhmcl\\.update_source\\.override=\\S*', $env:HMCL_UPDATE_JAVA_OPTION} Else {\\"$env:_TEMP_JAVA_TOOL_OPTIONS $env:HMCL_UPDATE_JAVA_OPTION\\"})"
                        )
                    )
                    """.replace("$$HMCL_UPDATE_JSON$$", url).replace("\n", "\r\n");

            if (FileUtils.writeIfChanged(jsonDir.resolve(channel.name() + ".bat"), res)) {
                changed = true;
            }
        }

        if (changed) {
            GitHubUtils.addEnv("COMMIT_CHANGE", "true");
        }
    }
}
