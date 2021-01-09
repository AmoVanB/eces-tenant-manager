package de.tum.ei.lkn.eces.tenantmanager.rerouting;

public enum LimitReroutingTypes {
    PERCENT,  // rerouteLimit is in % of the total nb of flows that can be rerouted
    ABSOLUTE, // rerouteLimit is in max nb of reroutings to try
}
