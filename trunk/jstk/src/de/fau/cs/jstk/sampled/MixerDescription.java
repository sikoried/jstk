package de.fau.cs.jstk.sampled;

import java.io.Serializable;

import javax.sound.sampled.Mixer;

/**
 * class intended for saving information (with XMLEncoder) about mixers used for playback or recording
 * 
 * @author hoenig
 *
 */
public class MixerDescription implements Serializable{	
	private static final long serialVersionUID = 1565291025106904209L;
	private String description = null;
	private String name = null;
	private String vendor = null;
	private String version = null;
	public MixerDescription(){};
	public MixerDescription(String description, String name, String vendor, String version){
		this.setDescription(description);
		this.setName(name);
		this.setVendor(vendor);
		this.setVersion(version);
	}
	
	public MixerDescription(Mixer.Info info){
		this(info.getDescription(),
			info.getName(),
			info.getVendor(),
			info.getVersion());
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	public String getVendor() {
		return vendor;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersion() {
		return version;
	}
}