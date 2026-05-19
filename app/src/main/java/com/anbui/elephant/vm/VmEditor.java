package com.anbui.elephant.vm;

import com.vectras.vm.AppConfig;
import com.vectras.vm.VMManager;
import com.vectras.vm.main.core.PendingCommand;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.JSONUtils;

public class VmEditor {
    public static boolean handle() {
        if (
                PendingCommand.vmId == null ||
                PendingCommand.vmConfig == null ||
                PendingCommand.paramsNotebookConfig == null
        ) return false;

        return handle(PendingCommand.vmId, PendingCommand.vmConfig, PendingCommand.paramsNotebookConfig, PendingCommand.forceCreate);
    }

    public static boolean handle(String vmId, String vmConfig, String paramsNotebookConfig, boolean forceCreate) {
        if (!FileUtils.isFileExists(AppConfig.romsdatajson))
            if (!FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]")) return false;

        if (!JSONUtils.isValidFromFile(AppConfig.romsdatajson))  return false;

        String vmIdReady = vmId.isEmpty() ? VMManager.idGenerator() : vmId;
        if (!forceCreate && VMManager.isVMExist(vmIdReady)) {
            if (!VMManager.replaceToVMList(-1, vmIdReady , vmConfig)) return false;
        } else {
            if (!FileUtils.isEmpty(VmFileManager.quickGetPath(vmIdReady ))) vmIdReady  = VMManager.idGenerator();

            if (!VMManager.addToVMList(vmConfig, vmIdReady )) return false;

            PendingCommand.forceCreate = true;
        }

        FileUtils.writeToFile(VmFileManager.getPath(vmIdReady), VmFileManager.CREATE_COMMAND_CONFIG_FILE_NAME, paramsNotebookConfig);

        return true;
    }
}
