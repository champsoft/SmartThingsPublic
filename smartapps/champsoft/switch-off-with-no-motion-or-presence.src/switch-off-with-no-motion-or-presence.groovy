/**
 *  Lights Off with No Motion and Presence
 *
 *  Author: Bruce Adelsman
 */

/**
 * Edited by Champsoft
 * Made presence sensors optional
 */
definition(
    name: "Switch Off with No Motion or Presence",
    namespace: "champsoft",
    author: "Bruce Adelsman",
    description: "Turn Switches off when no motion or presence is detected for a set period of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
	section("Switches to turn off") {
		input "switches", "capability.switch", title: "Choose device switches", multiple: true
	}
	section("Turn off when there is no motion or presence") {
		input "motionSensor", "capability.motionSensor", title: "Choose motion sensor"
   		input "presenceSensors", "capability.presenceSensor", title: "Choose presence sensors", multiple: true, required: false
	}
	section("Delay before turning off") {                    
		input "delayMins", "number", title: "Minutes of inactivity?"
	}
}

def installed() {
	subscribe(motionSensor, "motion", motionHandler)
    if(presenceSensors) {
		subscribe(presenceSensors, "presence", presenceHandler)
	}
}

def updated() {
	unsubscribe()
	subscribe(motionSensor, "motion", motionHandler)
    if(presenceSensors) {
		subscribe(presenceSensors, "presence", presenceHandler)
	}
}

def motionHandler(evt) {
	log.debug "handler $evt.name: $evt.value"
	if (evt.value == "inactive") {
		runIn(delayMins * 60, scheduleCheck, [overwrite: false])
	}
}

def presenceHandler(evt) {
	log.debug "handler $evt.name: $evt.value"
	if (evt.value == "not present") {
		runIn(delayMins * 60, scheduleCheck, [overwrite: false])
	}
}

def isActivePresence() {
	// check all the presence sensors, make sure none are present
	if(presenceSensors) {
		def noPresence = presenceSensors.find{it.currentPresence == "present"} == null
        !noPresence
	}	
}

def scheduleCheck() {
	log.debug "scheduled check"
	def motionState = motionSensor.currentState("motion")
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.rawDateCreated.time
    	def threshold = 1000 * 60 * delayMins - 1000
    	
        if (elapsed >= threshold) {
			if (!isActivePresence()) {
				log.debug "Motion has stayed inactive since last check ($elapsed ms) and no presence:  turning switches off"
                switches.findAll {
                	if (it.currentSwitch == "on") {
                    	log.debug "Turning off switch: ${it.label}"
  	                 	it.off()
                    }
               }
			}
			else {
            	log.debug "Presence is active: do nothing"
            }
    	} else {
        	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms): do nothing"
        }
    } else {
    	log.debug "Motion is active: do nothing"
    }
}