package com.daesabu.meongcoach.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.SpringBean;

final class ExternalSystemDiagram {

	private static final List<String> ADAPTER_PACKAGES = List.of(".adapter.integration", ".adapter.consumer");
	private static final String END_OF_DIAGRAM = "@enduml";
	private static final String STYLE = """
			<style>
			  cloud {
			    BackgroundColor: #f5f5f5;
			    LineColor: #444444;
			    LineThickness: 2;
			    FontColor: #444444;
			    FontSize: 20;
			  }
			  arrow {
			    LineColor: #444444;
			    LineThickness: 2;
			    FontColor: #444444;
			    FontSize: 18;
			  }
			</style>
			""";

	private ExternalSystemDiagram() {
	}

	static void appendTo(Path diagram, ApplicationModules modules) throws IOException {
		String content = Files.readString(diagram);
		List<String> relationships = new ArrayList<>();
		Set<ExternalSystem> systems = new LinkedHashSet<>();
		for (ApplicationModule module : modules) {
			Set<ExternalSystem> connected = connectedSystems(module);
			if (connected.isEmpty()) {
				continue;
			}
			String moduleAlias = moduleAlias(content, module);
			connected.forEach(system -> relationships.add(system.relationship(moduleAlias)));
			systems.addAll(connected);
		}
		List<String> declarations = systems.stream()
				.map(ExternalSystem::declaration)
				.toList();
		String externals = STYLE + String.join("\n", declarations) + "\n\n" + String.join("\n", relationships) + "\n\n";
		String appended = content.replace(END_OF_DIAGRAM, externals + END_OF_DIAGRAM);
		Files.writeString(diagram, appended);
	}

	private static Set<ExternalSystem> connectedSystems(ApplicationModule module) {
		Set<ExternalSystem> systems = new LinkedHashSet<>();
		module.getSpringBeans().stream()
				.filter(ExternalSystemDiagram::isAdapter)
				.forEach(bean -> systems.addAll(systemsConnectedBy(bean)));
		return systems;
	}

	private static boolean isAdapter(SpringBean bean) {
		String packageName = bean.getType().getPackageName();
		return ADAPTER_PACKAGES.stream().anyMatch(packageName::endsWith);
	}

	private static List<ExternalSystem> systemsConnectedBy(SpringBean bean) {
		return Arrays.stream(ExternalSystem.values())
				.filter(system -> system.isConnectedBy(bean))
				.toList();
	}

	private static String moduleAlias(String content, ApplicationModule module) {
		String marker = "component \"==" + module.getDisplayName() + "\\n";
		return content.lines()
				.filter(line -> line.trim().startsWith(marker))
				.map(line -> line.substring(line.lastIndexOf(" as ") + " as ".length()).trim())
				.findFirst()
				.orElseThrow();
	}
}
