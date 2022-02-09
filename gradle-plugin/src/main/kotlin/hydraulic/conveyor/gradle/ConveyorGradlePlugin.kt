package hydraulic.conveyor.gradle

import dev.hydraulic.types.machines.CLibraries
import dev.hydraulic.types.machines.LinuxMachine
import dev.hydraulic.types.machines.Machine
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.util.*

/**
 * Adds tasks that generate Conveyor configuration snippets based on the Gradle project to which it's applied.
 */
@Suppress("unused")
class ConveyorGradlePlugin : Plugin<Project> {
    private val machineConfigs = HashMap<Machine, Configuration>()
    private val currentMachine = Machine.current()

    private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun machineConfig(project: Project, machine: Machine): Configuration {
        return machineConfigs.getOrPut(machine) {
            val isCurrent = machine == currentMachine
            var configName = "${machine.os.identifier}${machine.cpu.identifier.capitalize()}"
            if (machine is LinuxMachine && machine.cLibrary != CLibraries.GLIBC)
                configName += machine.cLibrary.identifier.capitalize()
            val impl = project.configurations.asMap["implementation"]!!
            project.configurations.create(configName).also {
                if (isCurrent)
                    impl.extendsFrom(it)
            }
        }
    }

    override fun apply(project: Project) {
        // Supply configurations for all the supported machines. Type safe accessors will be created.
        for (m in setOf(
            Machine.LINUX_AARCH64, Machine.LINUX_AMD64, Machine.LINUX_AARCH64_MUSLC, Machine.LINUX_AMD64_MUSLC,
            Machine.WINDOWS_AMD64, Machine.WINDOWS_AARCH64,
            Machine.MACOS_AMD64, Machine.MACOS_AARCH64
        )) machineConfig(project, m)

        project.tasks.register("writeConveyorConfig", WriteConveyorConfigTask::class.java) {
            it.machineConfigs = machineConfigs
            it.destination.set(project.layout.projectDirectory.file("generated.conveyor.conf"))
        }
        project.tasks.register("printConveyorConfig", PrintConveyorConfigTask::class.java) {
            it.machineConfigs = machineConfigs
        }
    }
}
