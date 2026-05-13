package tech.lin2j.idea.plugin.ssh;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.model.DeployProfile;
import tech.lin2j.idea.plugin.model.UploadProfile;
import tech.lin2j.idea.plugin.uitl.CommandUtil;

import java.util.Objects;

/**
 * @author linjinjia
 * @date 2024/5/2 17:11
 */
public class SshUploadTask implements Runnable {
    private final int sshId;
    private final int profileId;
    private final ConsoleView console;
    private final Project project;

    private UploadProfile profile;
    private SshServer server;
    private String taskName;

    public SshUploadTask(ConsoleView console, DeployProfile deployProfile, @Nullable Project project) {
        this.sshId = deployProfile.getSshId();
        this.profileId = deployProfile.getProfileId();
        this.console = console;
        this.project = project;
        this.server = getServer();
        this.profile = getProfile();

        this.taskName = String.format("%s - %s", server.getIp(), profile.getName());
    }

    public SshUploadTask(ConsoleView console, int sshId, int profileId, @Nullable Project project) {
        this.console = console;
        this.sshId = sshId;
        this.profileId = profileId;
        this.project = project;
    }

    @Override
    public void run() {
        UploadProfile profile = getProfile();
        SshServer server = getServer();
        ConsoleCommandLog commandLog = new ConsoleCommandLog(console);
        CommandUtil.executeUpload(profile, server, commandLog, project);
    }

    public UploadProfile getProfile() {
        if (profile == null) {
            profile = ConfigHelper.getOneUploadProfileById(sshId, profileId);
        }
        Objects.requireNonNull(profile);
        return profile;
    }

    public SshServer getServer() {
        if (server == null) {
            server = ConfigHelper.getSshServerById(sshId);
        }
        Objects.requireNonNull(server);
        return server;
    }

    public String getTaskName() {
        return taskName;
    }
}