#include "player.c"


int create_pipeline(CustomData *);


int main(int argc, char *argv[]) {
  CustomData data;
  GstStateChangeReturn ret;
  GstBus *bus;
  GstMessage* msg;  
 
  /* Initialize GTK */
  gtk_init (&argc, &argv);
   
  /* Initialize GStreamer */
  gst_init (&argc, &argv);
   
  /* Initialize our data structure */
  memset (&data, 0, sizeof (data));
  data.duration = GST_CLOCK_TIME_NONE;
  data.rate = 1; 
     
    if(create_pipeline(&data) != 0) {
        g_error("COULD NOT MAKE PIPELINE");
        return -1;
    }

  /* Set the URI to play */
  //g_object_set (data.pipeline, "uri", "http://docs.gstreamer.com/media/sintel_trailer-480p.webm", NULL);
   
   /* Connect to interesting signals in pipeline */
  /*g_signal_connect (G_OBJECT (data.pipeline), "video-tags-changed", (GCallback) tags_cb, &data);
  g_signal_connect (G_OBJECT (data.pipeline), "audio-tags-changed", (GCallback) tags_cb, &data);
  g_signal_connect (G_OBJECT (data.pipeline), "text-tags-changed", (GCallback) tags_cb, &data);*/
  /* Create the GUI */
  create_ui (&data);
   
  /* Instruct the bus to emit signals for each received message, and connect to the interesting signals */
  bus = gst_element_get_bus (data.pipeline);
  gst_bus_add_signal_watch (bus);
  g_signal_connect (G_OBJECT (bus), "message::error", (GCallback)error_cb, &data);
  g_signal_connect (G_OBJECT (bus), "message::eos", (GCallback)eos_cb, &data);
  g_signal_connect (G_OBJECT (bus), "message::state-changed", (GCallback)state_changed_cb, &data);
  gst_bus_set_sync_handler (bus, (GstBusSyncHandler) bus_sync_handler, NULL);
  g_signal_connect (G_OBJECT (bus), "message::application", (GCallback)application_cb, &data);
  gst_object_unref (bus);
   
  /* Start playing */
  //ret = gst_element_set_state (data.pipeline, GST_STATE_PLAYING);
  /*if (ret == GST_STATE_CHANGE_FAILURE) {
    g_printerr ("Unable to set the pipeline to the playing state.\n");
    gst_object_unref (data.pipeline);
    return -1;
  }*/
   
  /* Register a function that GLib will call every second */
  g_timeout_add_seconds (1, (GSourceFunc)refresh_ui, &data);
   
  /* Start the GTK main loop. We will not regain control until gtk_main_quit is called. */
  gtk_main ();
  msg = gst_bus_timed_pop_filtered(bus, 100000* GST_MSECOND, GST_MESSAGE_EOS);

  /* Free resources */
  gst_element_set_state (data.pipeline, GST_STATE_NULL);
  gst_object_unref (data.pipeline);
  return 0;
}

/* This function will be called by the pad-added signal */
static void pad_added_handler (GstElement *src, GstPad *new_pad,gboolean b,  CustomData *data) {

  

  GstPad *sink_pad = gst_element_get_static_pad (data->videosink, "sink");
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

  if (!g_str_has_prefix (new_pad_type, "audio/x-raw-int")) {
    g_print ("  It has type '%s' which is not raw audio. Ignoring.\n", new_pad_type);
    //goto exit;
  }
  else {
	sink_pad = gst_element_get_static_pad (data->audiosink, "sink");
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


int create_pipeline(CustomData * data)
{
    /* Create the elements */
   //data->pipeline = gst_element_factory_make ("playbin2", "playbin2");
   
   data->pipeline = gst_pipeline_new("test-pipeline");
   data->src = gst_element_factory_make("filesrc", "fileSource");
   data->decode = gst_element_factory_make("decodebin2", "decodebin2"); 
   data->videosink = gst_element_factory_make("xvimagesink", "applicationsink");
   data->audiosink = gst_element_factory_make("alsasink", "audiosink");
    if( data->pipeline == NULL || data->src == NULL || data->videosink == NULL)
        return -1;
    //g_object_set(data->src, "location", "/home/rbhatia5/Documents/School/cs414/whitewater/MP1/H264_test1_Talkinghead_mp4_480x360.mp4",NULL);

    //Add all my elements to the bin
    gst_bin_add_many(GST_BIN(data->pipeline), data->src, data->decode, data->videosink,data->audiosink, NULL);
    gst_element_link_many(data->src, data->decode, NULL);
    //gst_element_set_state(data->pipeline, GST_STATE_PLAYING);
    
    g_signal_connect(data->decode, "new-decoded-pad", G_CALLBACK (pad_added_handler), data);



  return 0;


}

