package com.merlos.powerdetector.domain

import java.io.Serializable

enum class ActionType : Serializable {
    SMS,
    TELEGRAM
}

enum class PowerTrigger : Serializable {
    ON_AC_POWER,
    ON_BATTERY
}
