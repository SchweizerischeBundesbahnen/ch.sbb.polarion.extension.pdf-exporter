package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class EnumValuesProvider {

    private EnumValuesProvider() {
        // Utility class
    }

    @NotNull
    public static List<String> getAllLinkRoleNames(@NotNull ITrackerProject trackerProject) {
        List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
        final List<ILinkRoleOpt> linkRoles = getLinkRoles(trackerProject, wiTypes);
        return linkRoles.stream()
                .map(ILinkRoleOpt::getName)
                .toList();
    }

    @NotNull
    public static List<String> getBidirectionalLinkRoleNames(@NotNull ITrackerProject trackerProject, @Nullable List<String> directRoleNames) {
        if (directRoleNames != null && !directRoleNames.isEmpty()) {
            List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
            final List<ILinkRoleOpt> linkRoles = getLinkRoles(trackerProject, wiTypes);
            return linkRoles.stream()
                    .filter(linkRole -> directRoleNames.contains(linkRole.getName()))
                    .flatMap(linkRole -> Stream.of(linkRole.getName(), linkRole.getOppositeName()))
                    .toList();
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull
    public static List<ILinkRoleOpt> getLinkRoles(@NotNull ITrackerProject trackerProject, @NotNull List<ITypeOpt> wiTypes) {
        Set<ILinkRoleOpt> linkRoles = new LinkedHashSet<>();

        for (ITypeOpt wiType : wiTypes) {
            Collection<ILinkRoleOpt> roles = trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions(wiType.getId());
            linkRoles.addAll(roles);
        }

        return linkRoles.stream().toList();
    }

}
