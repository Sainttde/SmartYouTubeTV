package com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.mpdbuilder;

import android.util.Xml;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.misc.SimpleYouTubeMediaItem;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.misc.YouTubeMediaItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MyMPDParser implements MPDParser {
    // We don't use namespaces
    private static final String ns = null;
    private static final String TAG_MPD = "MPD";
    private static final String TAG_MEDIA_ITEM = "Representation";
    private static final String TAG_BASE_URL = "BaseURL";
    private static final String TAG_MEDIA_GROUP = "AdaptationSet";
    private static final String TAG_SEGMENT_BASE = "SegmentBase";
    private static final String TAG_INITIALIZATION = "Initialization";
    private static final String TAG_SEGMENT_LIST = "SegmentList";
    private InputStream mMpdContent;
    private XmlPullParser mParser;

    public MyMPDParser(InputStream mpdContent) {
        try {
            initParser(mpdContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initParser(InputStream mpdContent) throws XmlPullParserException, IOException {
        mMpdContent = mpdContent;
        mParser = Xml.newPullParser();
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        mParser.setInput(mpdContent, null);
        mParser.nextTag();
    }

    @Override
    public List<YouTubeMediaItem> parse() {
        if (mMpdContent == null) {
            return new ArrayList<>();
        }
        List<YouTubeMediaItem> result = null;
        try {
            result = readDashMPD();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<YouTubeMediaItem> readDashMPD() throws IOException, XmlPullParserException {
        List<YouTubeMediaItem> mediaItems = new ArrayList<>();

        mParser.require(XmlPullParser.START_TAG, ns, TAG_MPD);
        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = mParser.getName();
            // Starts by looking for the entry tag
            if (name.equals(TAG_MEDIA_GROUP)) {
                mediaItems.addAll(readMediaGroup(mParser));
            }
        }
        return mediaItems;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private List<YouTubeMediaItem> readMediaGroup(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_MEDIA_GROUP);
        List<YouTubeMediaItem> mediaItems = new ArrayList<>();
        String mimeType = parser.getAttributeValue(ns, "mimeType");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_MEDIA_ITEM)) {
                mediaItems.add(readMediaItem(parser, mimeType));
            } else {
                skip(parser);
            }
        }
        return mediaItems;
    }

    private YouTubeMediaItem readMediaItem(XmlPullParser parser, String mimeType) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_MEDIA_ITEM);
        YouTubeMediaItem item = new SimpleYouTubeMediaItem();

        // common tags
        item.setITag(parser.getAttributeValue(ns, "id"));
        item.setType(String.format("%s;+codecs=\"%s\"", mimeType, parser.getAttributeValue(ns, "codecs")));
        item.setBitrate(parser.getAttributeValue(ns, "bandwidth"));

        String frameRate = parser.getAttributeValue(ns, "frameRate");
        if (frameRate != null) { // video tags
            item.setFps(frameRate);
            String width = parser.getAttributeValue(ns, "width");
            String height = parser.getAttributeValue(ns, "height");
            String size = String.format("%sx%s", width, height);
            item.setSize(size);
        } else { // audio tags
            item.setAudioSamplingRate(parser.getAttributeValue(ns, "audioSamplingRate"));
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_BASE_URL)) {
                readUrl(parser, item);
            } else if (name.equals(TAG_SEGMENT_BASE)) {
                readSegmentBase(parser, item);
            } else if (name.equals(TAG_SEGMENT_LIST)) {
                readSegmentList(parser, item);
            } else {
                skip(parser);
            }
        }
        return item;
    }

    private void readSegmentList(XmlPullParser parser, YouTubeMediaItem item) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_SEGMENT_LIST);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_INITIALIZATION)) {
                readInitialization(parser, item);
            } else {
                skip(parser);
            }
        }
    }

    private void readSegmentBase(XmlPullParser parser, YouTubeMediaItem item) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_SEGMENT_BASE);
        item.setIndex(parser.getAttributeValue(ns, "indexRange"));
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_INITIALIZATION)) {
                readInitialization(parser, item);
            } else {
                skip(parser);
            }
        }
    }

    private void readInitialization(XmlPullParser parser, YouTubeMediaItem item) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_INITIALIZATION);
        String range = parser.getAttributeValue(ns, "range");
        if (range == null) { // 4k@60fps
            // sourceURL="range/0-712"
            // extremal situation: sourceURL="sq/0" (don't parse at all)
            String sourceURL = parser.getAttributeValue(ns, "sourceURL");
            range = sourceURL.replaceAll("range\\/", "");
            range = sourceURL.equals(range) ? null : range;
        }
        item.setInit(range);
        parser.next();
        parser.require(XmlPullParser.END_TAG, ns, TAG_INITIALIZATION);
    }

    private void readUrl(XmlPullParser parser, YouTubeMediaItem item) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_BASE_URL);
        item.setClen(parser.getAttributeValue(ns, "yt:contentLength"));
        String url = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, TAG_BASE_URL);
        item.setUrl(url);
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
}
