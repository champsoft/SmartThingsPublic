/**
 *  Lights Off with No Motion and Presence
 *
 *  Author: Bruce Adelsman
 */

/**
 * Edited by Champsoft
 * Made presence sensors optional and include multiple motion sensors
 */
definition(
    name: "Switch Off with No Motion or Presence",
    namespace: "champsoft",
    author: "pchampsoft",
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
		input "motionSensors", "capability.motionSensor", title: "Choose motion sensor", multiple: true
   		input "presenceSensors", "capability.presenceSensor", title: "Choose presence sensors", multiple: true, required: false
	}
	section("Delay before turning off") {                    
		input "delayMins", "number", title: "Minutes of inactivity?"
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	//subscribe(switches, "switch", switchHandler)
	//subscribe(motionSensors, "motion", motionHandler)
    if(presenceSensors) {
		//subscribe(presenceSensors, "presence", presenceHandler)
	}
	schedule("0 0/$delayMins * * * ?", scheduleCheck)
}

def switchHandler(evt) {
	log.debug "handler for $evt.displayName -> $evt.name: $evt.value"
	/*if (evt.value == "on") {
		runIn(delayMins * 60, scheduleCheck)
	}*/
}

def motionHandler(evt) {
	//log.debug "handler for $evt.displayName -> $evt.name: $evt.value"
	/*if (evt.value == "inactive") {
		runIn(delayMins * 60, scheduleCheck)
	}*/
}

def presenceHandler(evt) {
	//log.debug "handler $evt.name: $evt.value"
	/*if (evt.value == "not present") {
		runIn(delayMins * 60, scheduleCheck, [overwrite: false])
	}*/
}

def isActivePresence() {
	// check all the presence sensors, make sure none are present
	if(presenceSensors) {
		def noPresence = presenceSensors.find{it.currentPresence == "present"} == null
        !noPresence
	}	
}

def scheduleCheck() {
	log.debug "Scheduled $delayMins minute check for no motion"
    
    def allInactive = true
    def latestTime = 0
    
	motionSensors.findAll{ sensor ->
    	def motionState = sensor.currentState("motion")
        //Set flag to false if one of the sensors is active
        allInactive = motionState.value=="active"?false:allInactive
        
        def time = motionState.rawDateCreated.time
        //When multiple sensors, use the latest inactive sensor
        if(time > latestTime) {
        	latestTime = time
        }
        log.debug "The time for $sensor.label = $time and it is $sensor.currentMotion"    
    }
    
    if(allInactive) {
    	def elapsed = now() - latestTime
        def threshold = 1000 * 60 * delayMins - 1000

    	if (elapsed >= threshold) {
			if (!isActivePresence()) {
				log.debug "Motion has stayed inactive since last check ($elapsed ms) and no presence: attempting to turn switches off"
                switches.findAll {
                	if (it.currentSwitch == "on") {
                    	log.debug "Turning off switch: ${it.label}"
  	                 	it.off()
                    }
                    else {
                    	log.debug "Switch: ${it.label} is already off. No action taken."
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
