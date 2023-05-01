+ BusPlug {

	initMonitor { | vol |
		if(monitor.isNil) {
			monitor = Monitor.new;
			this.changed(\monitor);
		};
		if (vol.notNil) { monitor.vol_(vol) };
		^monitor
	}

}
