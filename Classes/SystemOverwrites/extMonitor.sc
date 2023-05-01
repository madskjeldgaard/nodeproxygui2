+ Monitor {

	vol_ { | val |
		if(val == vol) { ^this };
		vol = val;
		if(this.isPlaying) { group.set(\vol, val) };
		this.changed(\vol, [val]);
	}

}
