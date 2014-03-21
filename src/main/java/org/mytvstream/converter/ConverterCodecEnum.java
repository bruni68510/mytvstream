package org.mytvstream.converter;

/**
 * Contains the list of available codecs the use with an IConverter.
 * @author cbrunner
 *
 */
public enum ConverterCodecEnum {
	
	// video codecs
	H264("h264"),	
	THEORA("theora"),
	FLV1("flv"),
	// audio codecs
	AAC("aac"),
	MP3("mp3"),
	VORBIS("vorbis");
	
	/**
     * @param text
     */
    ConverterCodecEnum(final String text) {
        this.text = text;
    }

    private final String text;

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
