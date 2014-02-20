#include "CustomData.h"
#include <string.h>
static CustomData data;
static Monitor monitor;
static PlayerControls player_controls;
static GstBuffer *bufferi;
static GstBuffer *bufferf;
static gsize lasti = 0;
static gsize lastf = 1;
static GstClockTime t1 = 0;
static GstClockTime t2 = 0;

static int handshake =0;
/* This function will be called by the pad-added signal */
static void calculate_ratio(void){

	float ratio = (float) lastf/ (float)lasti;
	g_print("compression time: %u\ndecompression time: %u\nframe_size: %d\ncompression ratio: %f\n",t1, t2,lastf,ratio);
}

static void pad_added_handler (GstElement *src, GstPad *new_pad,gboolean b) 
{
  GstPad *sink_pad = gst_element_get_static_pad (monitor.tee2, "sink");
  GstPadLinkReturn ret;
  GstCaps *new_pad_caps = NULL;
  GstStructure *new_pad_struct = NULL;
  const gchar *new_pad_type = NULL;
   
  g_print ("Received new pad '%s' from '%s':\n", GST_PAD_NAME (new_pad), GST_ELEMENT_NAME (src));
   
  /* If our converter is already linked, we have nothing to do here */
  if (gst_pad_is_linked (sink_pad)) {
    g_print ("  We are already linked. Ignoring.\n");
    goto exit;
  }
   
  /* Check the new pad's type */
  new_pad_caps = gst_pad_get_caps (new_pad);
  new_pad_struct = gst_caps_get_structure (new_pad_caps, 0);
  new_pad_type = gst_structure_get_name (new_pad_struct);
  if (!g_str_has_prefix (new_pad_type, "video/x-raw")) {
    g_print ("  It has type '%s' which is not raw video. Ignoring.\n", new_pad_type);
    //goto exit;
  }
  else {
	ret = gst_pad_link (new_pad, sink_pad);
	if (GST_PAD_LINK_FAILED (ret)) {
    		g_print ("  Type is '%s' but link failed.\n", new_pad_type);
  	} else {
    		g_print ("  Link succeeded (type '%s').\n", new_pad_type);
  	}
  }

  /* Attempt the link */

  ret = gst_pad_link (new_pad, sink_pad);
  if (GST_PAD_LINK_FAILED (ret)) {
    g_print ("  Type is '%s' but link failed.\n", new_pad_type);
  } else {
    g_print ("  Link succeeded (type '%s').\n", new_pad_type);
  }
   
exit:
  /* Unreference the new pad's caps, if we got them */
  if (new_pad_caps != NULL)
    gst_caps_unref (new_pad_caps);
   
  /* Unreference the sink pad */
  gst_object_unref (sink_pad);
}


static void new_buffer_stream (GstElement *sink, int i) {
  GstBuffer *buffer;
   
  /* Retrieve the buffer */
  g_signal_emit_by_name (sink, "pull-buffer", &buffer);
  

  if (buffer) {
    /* The only thing we do in this example is print a * to indicate a received buffer */
    guint size = GST_BUFFER_SIZE(buffer); 
    //g_print ("*");
    g_print ("\n%d\n", size);
    gst_buffer_unref (buffer);
  }
}


static void new_buffer_playeri (GstElement *sink, int i) {
  
  /* Retrieve the buffer */
  g_signal_emit_by_name (sink, "pull-buffer", &bufferi);
  
  if(handshake ==0){
  if (bufferi) {
    /* The only thing we do in this example is print a * to indicate a received buffer */
    guint size = GST_BUFFER_SIZE(bufferi); 
    //g_print ("initial: %d\n", size);
    handshake = 1;
    lasti = size;
    t1 = GST_BUFFER_TIMESTAMP(bufferi);
    //gst_buffer_unref (buffer);
  }
}

  
}

static void new_buffer_playerf (GstElement *sink, int i) {
  
  g_signal_emit_by_name (sink, "pull-buffer", &bufferf);

  if(handshake == 1){
  	if (bufferf) {
    /* The only thing we do in this example is print a * to indicate a received buffer */
    	guint size = GST_BUFFER_SIZE(bufferf); 
    	//g_print ("final: %d\n", size);
    	lastf = size;

    //gst_buffer_unref (buffer);
  		}
  		t2 = GST_BUFFER_TIMESTAMP(bufferf);
  		calculate_ratio();
  		handshake = 0;
	}

  
}


/*
Author		: Zain, Kristian, Ramit
Function	: construct_pipeline
Purpose		: This function is called to construct a pipeline, from start to finish. 
Arguments	: CustomData structure that holds our pipeline
Returns		: T/F
*/

static gboolean start_streamer() {
			gchar *webcam_name;
			g_print("CONNECTING STREAM.\n");
			data.pipeline = gst_pipeline_new("stream-pipeline");
			data.source = gst_element_factory_make("v4l2src", "webcam");
			if(!data.source) {
				g_print("SOURCE FAILED.\n");
				return FALSE;
			}
			g_object_get(G_OBJECT(data.source),"device-name", &webcam_name, NULL);
			if(webcam_name!=NULL) {
			g_print("%s \n",webcam_name);
			data.sink2 = gst_element_factory_make("xvimagesink", "playersink");

			
			monitor.tee1 = gst_element_factory_make ("tee", "tee1");
			
			monitor.q1a = gst_element_factory_make ("queue","q1a"); //appsinki
			monitor.q1b = gst_element_factory_make ("queue","q1b"); //tee2

			
			monitor.appsinki = gst_element_factory_make("appsink", "appsinki");
			g_object_set (monitor.appsinki, "emit-signals", TRUE, NULL);
			g_signal_connect (monitor.appsinki, "new-buffer", G_CALLBACK (new_buffer_stream),0);			
/*

			g_object_set (monitor.appsinkf, "emit-signals", TRUE, NULL);
			g_signal_connect (monitor.appsinkf, "new-buffer", G_CALLBACK (new_bufferf),0);
*/


			//gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.sink2, NULL);
			
			gst_bin_add_many(GST_BIN(data.pipeline), data.source , data.sink2, monitor.tee1, monitor.q1a, monitor.q1b, monitor.appsinki, NULL);

				if (gst_element_link_many (data.source, monitor.tee1, NULL) != TRUE ||
				  gst_element_link_many (monitor.q1a, monitor.appsinki, NULL) != TRUE ||
				  gst_element_link_many (monitor.q1b, data.sink2, NULL) != TRUE
				  ){
						g_printerr ("Elements could not be linked.\n");
						//gst_object_unref (pipeline);
						//return -1;
			      }

			      g_print("Linking Pads together\n");

     			monitor.q1aPado = gst_element_get_request_pad (monitor.tee1,"src%d");
     			g_print ("Obtained request pad %s for q1aPado.\n", gst_pad_get_name (monitor.q1aPado));
     			monitor.q1aPadi = gst_element_get_static_pad (monitor.q1a, "sink");

     			     if (gst_pad_link (monitor.q1aPado,monitor.q1aPadi) != GST_PAD_LINK_OK) {
     				g_printerr ("Tee1 could not be linked to q1a.\n");
     				//gst_object_unref (pipeline);
     				//return -1;
     			      }

     			monitor.q1bPado = gst_element_get_request_pad (monitor.tee1,"src%d");
     			g_print ("Obtained request pad %s for q1aPado.\n", gst_pad_get_name (monitor.q1bPado));
     			monitor.q1bPadi = gst_element_get_static_pad (monitor.q1b, "sink");

     			    if (gst_pad_link (monitor.q1bPado, monitor.q1bPadi) != GST_PAD_LINK_OK) {
     				g_printerr ("Tee1 could not be linked to q1b.\n");
     				//gst_object_unref (pipeline);
     				//return -1;
     			      }

			      gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
/*
			if(!gst_element_link(data.source, data.sink2))
				g_print("Not Streaming");
			else
				g_print("Pipeline created");
			gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
			*/}
}

static gboolean start_player(char* filename) {
			data.pipeline = gst_pipeline_new("player-pipeline");
            data.source = gst_element_factory_make("filesrc", "fileSource");
            g_object_set (data.source, "location", filename, NULL);
            data.decoder = gst_element_factory_make("decodebin2", "decodebin2"); 
            data.sink2 = gst_element_factory_make("xvimagesink", "applicationsink");
            g_signal_connect(data.decoder, "new-decoded-pad", G_CALLBACK (pad_added_handler), NULL);
        //   gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.decoder, data.sink2, NULL);
           


            			monitor.tee1 = gst_element_factory_make ("tee", "tee1");
            			
            			monitor.q1a = gst_element_factory_make ("queue","q1a"); //appsinki
            			monitor.q1b = gst_element_factory_make ("queue","q1b"); //tee2

            			monitor.tee2 = gst_element_factory_make ("tee", "tee2");
            			
            			monitor.q2a = gst_element_factory_make ("queue","q2a"); //appsinki
            			monitor.q2b = gst_element_factory_make ("queue","q2b"); //tee2

            			
            			monitor.appsinki = gst_element_factory_make("appsink", "appsinki");
            			
            			g_object_set (monitor.appsinki, "emit-signals", TRUE, NULL);
            			g_signal_connect (monitor.appsinki, "new-buffer", G_CALLBACK (new_buffer_playeri),0);			
           				

           				monitor.appsinkf = gst_element_factory_make("appsink", "appsinkf");
            			
            			g_object_set (monitor.appsinkf, "emit-signals", TRUE, NULL);
            			g_signal_connect (monitor.appsinkf, "new-buffer", G_CALLBACK (new_buffer_playerf),0);
           				

            			//gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.sink2, NULL);
         			
            			gst_bin_add_many(GST_BIN(data.pipeline), data.source ,data.decoder, data.sink2, monitor.tee1, monitor.q1a, monitor.q1b, monitor.appsinki,monitor.tee2, monitor.q2a,monitor.q2b, monitor.appsinkf, NULL);


            				if(!gst_element_link_many (data.source, monitor.tee1, NULL)){
            					g_print("1 was bad");
            				}

            				if(!gst_element_link_many (monitor.q1a, monitor.appsinki, NULL)){
            					g_print("2 was bad");
            				}


            				if(!gst_element_link_many (monitor.q1b,data.decoder, NULL)){
            					g_print("3 was bad");
            				}

            				if(!gst_element_link_many (data.decoder, monitor.tee2, NULL)){
            					g_print("3 was bad");
            				}

            				if(!gst_element_link_many (monitor.q2a,monitor.appsinkf, NULL)){
            					g_print("4 was bad");
            				}

            				if(!gst_element_link_many (monitor.q2b,data.sink2, NULL)){
            					g_print("5 was bad");
            				}

/*
            				if (gst_element_link_many (data.source, monitor.tee1, NULL) != TRUE ||
            				  gst_element_link_many (monitor.q1a, monitor.appsinki, NULL) != TRUE ||
            				  gst_element_link_many (monitor.q1b,data.decoder, monitor.tee2, NULL) != TRUE||
            				  gst_element_link_many (monitor.q2a,monitor.appsinkf, NULL)!= TRUE||
            				   gst_element_link_many (monitor.q2b,data.sink2, NULL)!= TRUE

            				  ){
            						g_printerr ("Elements could not be linked.\n");
            						//gst_object_unref (pipeline);
            						//return -1;
            			      }

*/          			      g_print("Linking Pads together\n");

                 			monitor.q1aPado = gst_element_get_request_pad (monitor.tee1,"src%d");
                 			g_print ("Obtained request pad %s for q1aPado.\n", gst_pad_get_name (monitor.q1aPado));
                 			monitor.q1aPadi = gst_element_get_static_pad (monitor.q1a, "sink");

                 			     if (gst_pad_link (monitor.q1aPado,monitor.q1aPadi) != GST_PAD_LINK_OK) {
                 				g_printerr ("Tee1 could not be linked to q1a.\n");
                 				//gst_object_unref (pipeline);
                 				//return -1;
                 			      }

                 			monitor.q1bPado = gst_element_get_request_pad (monitor.tee1,"src%d");
                 			g_print ("Obtained request pad %s for q1aPado.\n", gst_pad_get_name (monitor.q1bPado));
                 			monitor.q1bPadi = gst_element_get_static_pad (monitor.q1b, "sink");

                 			    if (gst_pad_link (monitor.q1bPado, monitor.q1bPadi) != GST_PAD_LINK_OK) {
                 				g_printerr ("Tee1 could not be linked to q1b.\n");
                 				//gst_object_unref (pipeline);
                 				//return -1;
                 			      }

                 			monitor.q2aPado = gst_element_get_request_pad (monitor.tee2,"src%d");
                 			g_print ("Obtained request pad %s for q2aPado.\n", gst_pad_get_name (monitor.q1aPado));
                 			monitor.q2aPadi = gst_element_get_static_pad (monitor.q2a, "sink");

                 			     if (gst_pad_link (monitor.q2aPado,monitor.q2aPadi) != GST_PAD_LINK_OK) {
                 				g_printerr ("Tee2 could not be linked to q2a.\n");
                 				//gst_object_unref (pipeline);
                 				//return -1;
                 			      }

                 			monitor.q2bPado = gst_element_get_request_pad (monitor.tee2,"src%d");
                 			g_print ("Obtained request pad %s for q2aPado.\n", gst_pad_get_name (monitor.q1bPado));
                 			monitor.q2bPadi = gst_element_get_static_pad (monitor.q2b, "sink");

                 			    if (gst_pad_link (monitor.q2bPado, monitor.q2bPadi) != GST_PAD_LINK_OK) {
                 				g_printerr ("Tee2 could not be linked to q2b.\n");
                 				//gst_object_unref (pipeline);
                 				//return -1;
                 			      }


/*
            if(gst_element_link_many(data.source, data.decoder, data.sink2, NULL)) {
		     }
			else {
				g_print("Invalid File \n");
			}*/
			gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static gboolean start_video_recorder() {
			gchar *webcam_name;
			GstPad *tee_player_pad, *tee_file_pad;
			GstPad *queue_player_pad, *queue_file_pad;
			g_print("RECORDING VIDEO.\n");
			data.pipeline = gst_pipeline_new("video-recorder-pipeline");
			data.source = gst_element_factory_make("v4l2src", "webcam");
			if(!data.source) {
				g_print("SOURCE FAILED.\n");
				return FALSE;
			}
			g_object_get(G_OBJECT(data.source),"device-name", &webcam_name, NULL);
			if(webcam_name!=NULL){
			data.enc_caps = gst_caps_new_simple (
				"video/x-raw-yuv", 
				"width", G_TYPE_INT, 320, 
				"height", G_TYPE_INT, 240, 
				"framerate", GST_TYPE_FRACTION, 20,1,
				NULL);
			if(!data.enc_caps){
				g_print("Caps structure could not be initialized.\n");
				return FALSE;
			}
	
			data.colorspace = gst_element_factory_make("ffmpegcolorspace", "cs");
			if(!data.colorspace) {				
				g_print("COLORSPACE FAILED.\n");
				return FALSE;
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
				g_print("ENCODER FAILED.\n");
				return FALSE;
			}

			data.mux = gst_element_factory_make("avimux", "mux");
			if(!data.mux)
			{
				g_print("MUX FAILED.\n");
				return FALSE;
			}
			data.sink = gst_element_factory_make("filesink", "filesink");
			if(!data.sink)
			{
				g_print("FSINK FAILED.\n");
				return FALSE;
			}
			
			//save recording video here
			g_object_set(G_OBJECT(data.sink), "location", "1.mp4",NULL);

			data.sink2 = gst_element_factory_make("xvimagesink", "playersink");

			data.tee = gst_element_factory_make("tee", "tee");
			data.player_queue = gst_element_factory_make("queue", "player-queue");
			data.file_queue = gst_element_factory_make("queue", "file-queue");

			gst_bin_add_many(GST_BIN(data.pipeline), data.source, data.colorspace, data.encoder, data.mux, data.tee, data.player_queue, data.file_queue, data.sink, data.sink2, NULL);
			gst_element_link(data.source, data.tee);
			//gboolean link_ok = gst_element_link_filtered (data.file_queue, data.colorspace, data.enc_caps);
			if(!gst_element_link_filtered (data.file_queue, data.colorspace, data.enc_caps))
				g_print("data.file_queue, data.colorspace, data.enc_caps link failed\n");
			if(gst_element_link_many(data.colorspace, data.encoder, data.mux, data.sink, NULL))
				g_print("ddata.colorspace, data.encoder, data.mux, data.sink link failed\n");
			if(gst_element_link_many(data.player_queue, data.sink2, NULL))
				g_print("data.player_queue, data.sink2 link failed\n");
	
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
			
			gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
			}
	}

static gboolean start_audio_recorder() {
			gchar *device_name;
			g_print("RECORDING AUDIO.\n");
			data.pipeline = gst_pipeline_new("audio-pipeline");
			data.source = gst_element_factory_make("alsasrc", "source");
			if(!data.source)
			{
				g_print("SOURCE FAILED.\n");
				return FALSE;
			}
			g_object_get(G_OBJECT(data.source),"device-name", &device_name, NULL);
			if(device_name!=NULL){
			g_object_set(data.source, "device", "hw:2", NULL);
			data.enc_caps = gst_caps_new_simple (
				"audio/x-raw-int", 
				"rate", G_TYPE_INT, 44100, 
				"channels", G_TYPE_INT, 1,
				NULL);
			if(!data.enc_caps)
			{
				g_print("Caps structure could not be initialized.\n");
				return FALSE;
			}
			data.sink = gst_element_factory_make("filesink", "filesink");
			if(!data.sink)
			{
				g_print("FSINK FAILED.\n");
				return FALSE;
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
			        				    
			}
			gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
		}
}

static gboolean disassemble_pipeline()
{
	if(data.pipeline!=NULL) {
	gst_element_set_state(data.pipeline, GST_STATE_READY);
	gst_element_set_state(data.pipeline, GST_STATE_NULL);
	gst_object_unref(data.pipeline);
	data.pipeline = NULL;
	}
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


