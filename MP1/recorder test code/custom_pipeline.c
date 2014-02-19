#include "CustomData.h"

static CustomData data;

/*
Author		: Zain, Kristian
Function	: construct_pipeline
Purpose		: This function is called to construct a pipeline, from start to finish. 
Arguments	: CustomData structure that holds our pipeline
Returns		: T/F
*/
static void assemble_pipeline()
{
	gboolean ret;
	GstPadTemplate *tee_src_pad_template;
	GstPad *tee_player_pad, *tee_file_pad;
	GstPad *queue_player_pad, *queue_file_pad;

	data.pipeline = gst_pipeline_new("test-pipeline");
	if(!data.pipeline)
	{
		ret = FALSE;
		g_print("Couldn't initialize pipeline.\n");
	}
	switch(data.Mode)
	{
	    case PLAYER:	    
	        data.pipeline = gst_pipeline_new("test-pipeline");
            data.source = gst_element_factory_make("filesrc", "fileSource");
            data.decoder = gst_element_factory_make("decodebin2", "decodebin2"); 
            data.sink2 = gst_element_factory_make("xvimagesink", "applicationsink");
            gst_bin_add_many(GST_BIN(data.pipeline), data.source,data.decoder, data.sink2, NULL);
            gst_element_link_many(data.source, data.decoder, data.sink2, NULL);
	        break;
	        
		case STREAM:
			g_print("CONNECTING STREAM.\n");
			data.pipeline = gst_pipeline_new("test-pipeline");
			data.source = gst_element_factory_make("v4l2src", "webcam");
			if(!data.source)
			{
				ret = FALSE;
				g_print("SOURCE FAILED.\n");
			}
			data.sink2 = gst_element_factory_make("xvimagesink", "playersink");

			gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.sink2, NULL);
			gst_element_link(data.source, data.sink2);
			break;

		case RECORD_VIDEO:
			g_print("RECORDING VIDEO.\n");
			data.pipeline = gst_pipeline_new("test-pipeline");
			data.source = gst_element_factory_make("v4l2src", "webcam");
			if(!data.source)
			{
				ret = FALSE;
				g_print("SOURCE FAILED.\n");
			}
			

			data.enc_caps = gst_caps_new_simple (
				"video/x-raw-yuv", 
				"width", G_TYPE_INT, 320, 
				"height", G_TYPE_INT, 240, 
				"framerate", GST_TYPE_FRACTION, 20,1,
				NULL);
				
			if(!data.enc_caps)
			{
				ret = FALSE;
				g_print("Caps structure could not be initialized.\n");
			}
	
			data.colorspace = gst_element_factory_make("ffmpegcolorspace", "cs");
			if(!data.colorspace)
			{
				ret = FALSE;
				g_print("COLORSPACE FAILED.\n");
			}

			switch(data.video_encoder)
			{
				case MJPEG:
					data.encoder = gst_element_factory_make("jpegenc", "encoder");
					break;
				case MPEG:
					data.encoder = gst_element_factory_make("ffenc_mpeg4", "encoder");
					break;
				default:
					break;
			}
			if(!data.encoder)
			{
				ret = FALSE;
				g_print("ENCODER FAILED.\n");
			}

			data.mux = gst_element_factory_make("avimux", "mux");
			if(!data.mux)
			{
				ret = FALSE;
				g_print("MUX FAILED.\n");
			}
			data.sink = gst_element_factory_make("filesink", "filesink");
			if(!data.sink)
			{
				ret = FALSE;
				g_print("FSINK FAILED.\n");
			}
			g_object_set(G_OBJECT(data.sink), "location", "1.mp4",NULL);

			data.sink2 = gst_element_factory_make("xvimagesink", "playersink");

			data.tee = gst_element_factory_make("tee", "tee");
			data.player_queue = gst_element_factory_make("queue", "player-queue");
			data.file_queue = gst_element_factory_make("queue", "file-queue");

			gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.colorspace, data.encoder, data.mux, data.tee, data.player_queue, data.file_queue, data.sink, data.sink2, NULL);
			gst_element_link(data.source, data.tee);
			//gboolean link_ok = gst_element_link_filtered (data.file_queue, data.colorspace, data.enc_caps);
			gst_element_link_filtered (data.file_queue, data.colorspace, data.enc_caps);
			gst_element_link_many(data.colorspace, data.encoder, data.mux, data.sink, NULL);
			gst_element_link_many(data.player_queue, data.sink2, NULL);
	
			//tee_src_pad_template = gst_element_get_compatible_pad_template (GST_ELEMENT_GET_CLASS (data.tee), "src%d");
			//tee_player_pad = gst_pad_new_from_template(tee_src_pad_template,"src%d");
			tee_player_pad = gst_element_get_request_pad (data.tee,"src%d");
			if(!tee_player_pad)
			    g_print("Could not get tee pad.\n");
			g_print ("Obtained request pad %s for player branch.\n", gst_pad_get_name (tee_player_pad));
			queue_player_pad = gst_element_get_static_pad (data.player_queue, "sink");
			tee_file_pad = gst_element_get_request_pad (data.tee, "src%d");
			g_print ("Obtained request pad %s for file branch.\n", gst_pad_get_name (tee_file_pad));
			queue_file_pad = gst_element_get_static_pad (data.file_queue, "sink");
			if (gst_pad_link (tee_player_pad, queue_player_pad) != GST_PAD_LINK_OK ||
				gst_pad_link (tee_file_pad, queue_file_pad) != GST_PAD_LINK_OK) {
				g_print("Tee could not be linked.\n");
			}
			gst_object_unref (queue_file_pad);
			gst_object_unref (queue_player_pad);
			break;

		case RECORD_AUDIO:
			g_print("RECORDING AUDIO.\n");
			data.pipeline = gst_pipeline_new("test-pipeline");
			data.source = gst_element_factory_make("alsasrc", "source");
			if(!data.source)
			{
				ret = FALSE;
				g_print("SOURCE FAILED.\n");
			}
			g_object_set(data.source, "device", "hw:2", NULL);
			
			data.enc_caps = gst_caps_new_simple (
				"audio/x-raw-int", 
				"rate", G_TYPE_INT, 44100, 
				"channels", G_TYPE_INT, 1,
				NULL);
			if(!data.enc_caps)
			{
				ret = FALSE;
				g_print("Caps structure could not be initialized.\n");
			}



			data.sink = gst_element_factory_make("filesink", "filesink");
			if(!data.sink)
			{
				ret = FALSE;
				g_print("FSINK FAILED.\n");
			}
            switch(data.audio_encoder)
			{
				case MULAW:
					data.encoder = gst_element_factory_make("mulawenc", "encoder");
					g_object_set(G_OBJECT(data.sink), "location", "audio_rec.mulaw",NULL);					
			        gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.encoder, data.sink, NULL);
			        gst_element_link_filtered (data.source, data.encoder, data.enc_caps);
			        gst_element_link_many(data.encoder, data.sink, NULL);
					break;
				case ALAW:
					data.encoder = gst_element_factory_make("alawenc", "encoder");
					g_object_set(G_OBJECT(data.sink), "location", "audio_rec.alaw",NULL);					
			        gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.encoder, data.sink, NULL);
			        gst_element_link_filtered (data.source, data.encoder, data.enc_caps);
			        gst_element_link_many(data.encoder, data.sink, NULL);					
					break;
				case MKV:
				    data.encoder = gst_element_factory_make("vorbisenc", "encoder");
				    data.mux = gst_element_factory_make("webmmux", "mux");
				    data.audioconvert = gst_element_factory_make("audioconvert", "audioconvert");
				    g_object_set(G_OBJECT(data.sink), "location", "audio_rec.mkv",NULL);
			        gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.audioconvert, data.encoder, data.mux, data.sink, NULL);
			        gst_element_link_many(data.source, data.audioconvert, data.encoder, data.mux, data.sink, NULL);				    
				    break;
				default:
					break;
		    }
			break;
		default:
			break;
	}
}

static void disassemble_pipeline()
{
	gst_element_set_state(data.pipeline, GST_STATE_READY);
	gst_element_set_state(data.pipeline, GST_STATE_NULL);
	g_object_unref(data.pipeline);
	//
	////unlink the elements
	//gst_element_set_state(data.sink, GST_STATE_NULL);
	//gst_element_unlink(data.mux, data.sink);
	//gst_bin_remove(GST_BIN(data.pipeline),data.sink);
	//

	////change the encoder
	//g_object_set(G_OBJECT(data.sink), "location", "2114.mp4",NULL);

	////relink the elements
	//gst_bin_add(GST_BIN(data.pipeline), data.sink);
	//gst_element_link(data.mux, data.sink);
	//gst_element_set_state(data.sink, GST_STATE_READY);
}