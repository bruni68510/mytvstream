package org.mytvstream.converter;

/**
 * Contains the list of available codecs the use with an IConverter.
 * @author cbrunner
 *
 */
public enum ConverterCodecEnum {
	H264("h264"),
	AAC("aac"),
	MP3("mp3"),
	FLV1("flv");
	
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
