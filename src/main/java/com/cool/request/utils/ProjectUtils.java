package com.cool.request.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.io.URLUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.cool.request.common.constant.CoolRequestConfigConstant.SCRIPT_NAME;

public class ProjectUtils {
    public static Project getCurrentProject() {
        return ProjectManager.getInstance().getOpenProjects()[0];
    }

    /**
     * 获取用户项目通过maven、gradle引入的第三方jar包
     *
     * @param project
     * @return
     */
    public static List<String> getUserProjectIncludeLibrary(Project project) {
        List<String> libraryNames = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager.getInstance(module).orderEntries().forEachLibrary(library -> {
                String[] urls = library.getUrls(OrderRootType.CLASSES);
                if (urls.length > 0) {
                    libraryNames.add(library.getUrls(OrderRootType.CLASSES)[0]);
                }
                return true;
            });
        }
        return libraryNames.stream().map(jarUrl -> {
            String jarFile = jarUrl;
            //上面获取的是jar://x.jar!/格式的数据，这里需要去除掉
            if (jarFile.startsWith("jar://") && jarFile.endsWith("!/")) {
                jarFile = jarFile.substring(6);
                jarFile = jarFile.substring(0, jarFile.length() - 2);
            }
            return jarFile;
        }).collect(Collectors.toList());
    }

    public static List<String> getClassOutputPaths(Project project) {
        List<String> result = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            try {
                String path = CompilerModuleExtension.getInstance(module).getCompilerOutputPath().getPath();
                result.add(path);
                //这里有的模块获取不到，直接忽略
            } catch (Exception e) {
            }
        }
        return result;
    }

    private static Module findMainModule(Project project) {
        Module result = null;
        int moduleNameLength = Integer.MAX_VALUE;
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (module.getName().length() < moduleNameLength) {
                moduleNameLength = module.getName().length();
                result = module;
            }
        }
        return result;
    }

    public static void addDependency(Project project, String jarPath) {
        Module[] modules = ModuleManager.getInstance(project).getModules();

        Module mainModule = findMainModule(project);
        if (mainModule == null && modules.length > 0) mainModule = modules[modules.length - 1];
        if (mainModule == null) return;

        LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
        final LibraryTable.ModifiableModel projectLibraryModel = projectLibraryTable.getModifiableModel();

        Library scriptLib = projectLibraryTable.getLibraryByName(SCRIPT_NAME);
        if (scriptLib == null) {
            scriptLib = projectLibraryModel.createLibrary(SCRIPT_NAME);
            String pathUrl = VirtualFileManager.constructUrl(URLUtil.JAR_PROTOCOL, jarPath + JarFileSystem.JAR_SEPARATOR);
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(pathUrl);
            Library.ModifiableModel libraryModel = scriptLib.getModifiableModel();
            libraryModel.addRoot(file, OrderRootType.CLASSES);
            Library finalScriptLib1 = scriptLib;
            ApplicationManager.getApplication().runWriteAction(() -> {
                ModuleRootModificationUtil.addDependency(findMainModule(project), finalScriptLib1, DependencyScope.COMPILE, false);
                libraryModel.commit();
                projectLibraryModel.commit();
            });
            return;
        }else {
        }

        ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(mainModule).getModifiableModel();
        LibraryTable moduleLibraryTable = modifiableModel.getModuleLibraryTable();

        if (moduleLibraryTable.getLibraryByName(SCRIPT_NAME) != null) return;
        Library finalScriptLib = scriptLib;

        ApplicationManager.getApplication().runWriteAction(() -> {
            ModuleRootModificationUtil.addDependency(findMainModule(project), finalScriptLib, DependencyScope.COMPILE, false);
            modifiableModel.commit();
        });
    }
}
