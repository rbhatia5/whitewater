#include "custom_pipeline.c"
static void attach_bus_cb();
static void send_seek_event (); 
static gboolean refresh_ui ();
static GstBusSyncReply bus_sync_handler (GstBus * bus, GstMessage * message);
static void delete_event_cb(GtkWidget * widget, GdkEvent * eventt);
static void play_cb(GtkWidget* widget, GdkEvent * eventt);
static void pause_cb(GtkWidget* widget, GdkEvent * eventt);
static void stop_cb(GtkWidget* widget, GdkEvent * eventt);
static void error_cb(GstBus * bus, GstMessage * msg);
static void state_changed_cb(GstBus *bus, GstMessage * msg);
static void eos_cb(GstBus * bus, GstMessage * msg);
static void record_audio_cb(GtkWidget* widget, GdkEvent * eventt);
static void record_video_cb(GtkWidget* widget, GdkEvent * eventt);
static void audio_encoding_cb(GtkWidget* widget, GdkEvent * eventt);
static void realize_cb(GtkWidget *widget);
static void application_cb (GstBus *bus, GstMessage *msg);

static GstBusSyncReply bus_sync_handler (GstBus * bus, GstMessage * message)
{
	// ignore anything but 'prepare-xwindow-id' element messages
	if (GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT)
		return GST_BUS_PASS;
	if (!gst_structure_has_name (message->structure, "prepare-xwindow-id"))
		return GST_BUS_PASS;

	if (video_window_xid != 0) {
		GstXOverlay *xoverlay;

		xoverlay = GST_X_OVERLAY (data.sink2);
		gst_x_overlay_set_xwindow_id (xoverlay, video_window_xid);
	} else {
		g_warning ("Should have obtained video_window_xid by now!");
	}

	gst_message_unref (message);
	return GST_BUS_DROP;
}

//activate the player
static void player_cb() {
gtk_widget_set_sensitive(player_controls.play_button, FALSE);
gtk_widget_set_sensitive(player_controls.pause_button, FALSE);
gtk_widget_set_sensitive(player_controls.stop_button, FALSE);
gtk_widget_set_sensitive(player_controls.fileopen_button, TRUE);
gtk_widget_set_sensitive(player_controls.fastforward_button, FALSE);
gtk_widget_set_sensitive(player_controls.fastrewind_button, FALSE);

gtk_widget_set_sensitive(player_controls.record_video_button, FALSE);
gtk_widget_set_sensitive(player_controls.record_audio_button, FALSE);
gtk_widget_set_sensitive(player_controls.audio_vbox, FALSE);
gtk_widget_set_sensitive(player_controls.video_vbox, FALSE);
//disable player button and enable recorder button
gtk_widget_set_sensitive(player_controls.recorder_button, TRUE);
gtk_widget_set_sensitive(player_controls.player_button, FALSE);
}

static void recorder_cb() {
gtk_widget_set_sensitive(player_controls.play_button, FALSE);
gtk_widget_set_sensitive(player_controls.pause_button, FALSE);
gtk_widget_set_sensitive(player_controls.stop_button, FALSE);
gtk_widget_set_sensitive(player_controls.fileopen_button, FALSE);
gtk_widget_set_sensitive(player_controls.fastforward_button, FALSE);
gtk_widget_set_sensitive(player_controls.fastrewind_button, FALSE);

gtk_widget_set_sensitive(player_controls.record_video_button, TRUE);
gtk_widget_set_sensitive(player_controls.record_audio_button, TRUE);
gtk_widget_set_sensitive(player_controls.audio_vbox, TRUE);
gtk_widget_set_sensitive(player_controls.video_vbox, TRUE);
//disable recorder button and enable player
gtk_widget_set_sensitive(player_controls.recorder_button, FALSE);
gtk_widget_set_sensitive(player_controls.player_button, TRUE);
disassemble_pipeline();
start_streamer();
}

static void delete_event_cb(GtkWidget * widget, GdkEvent * eventt)
{
	gtk_main_quit(); 
}

static void post_message();

static void play_cb(GtkWidget* widget, GdkEvent * eventt)
{
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static void pause_cb(GtkWidget* widget, GdkEvent * eventt)
{
	gst_element_set_state(data.pipeline, GST_STATE_PAUSED);
}

//stop player from playing
static void stop_cb(GtkWidget* widget, GdkEvent * eventt) {
	gst_element_set_state(data.pipeline, GST_STATE_READY);
}

//stop recording and save file
static void stop_recording_cb() {
	disassemble_pipeline();
	attach_bus_cb();
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static void error_cb(GstBus * bus, GstMessage * msg)
{
	GError *err;
	gchar *debug_info;
	gst_message_parse_error(msg, &err, &debug_info);
	g_printerr("Error message received from %s: %s\n", GST_OBJECT_NAME(msg->src), err->message);
	g_printerr("Debug Info: %s\n", debug_info ? debug_info : "none");
	g_clear_error(&err);
	//g_free(&debug_info);

	gst_element_set_state(data.pipeline, GST_STATE_READY);
}

static void eos_cb(GstBus * bus, GstMessage * msg)
{
	g_print("Reached End of Stream\n");
	gst_element_set_state(data.pipeline, GST_STATE_READY);
	if(data.Mode == PLAYER)
	{
	    data.Mode = STREAM;
	}
	disassemble_pipeline();
	attach_bus_cb();
}

//change of state handler
static void state_changed_cb(GstBus *bus, GstMessage * msg)
{
	//parse what change of state occurred, print it
	GstState old_state, new_state, pending_state;
	gst_message_parse_state_changed(msg, &old_state, &new_state, &pending_state);
	if(GST_MESSAGE_SRC(msg) == GST_OBJECT(data.pipeline))
	{
		g_print("State change from %s to %s\n", gst_element_state_get_name(old_state), gst_element_state_get_name(new_state));
		data.state = new_state;
		if (old_state == GST_STATE_READY && new_state == GST_STATE_PAUSED) 
		{
            refresh_ui (&data);
        }
	}
}

static void attach_bus_cb()
{
	//attach handler to assign window xid to stream to window
	data.bus = gst_pipeline_get_bus (GST_PIPELINE (data.pipeline));
	gst_bus_set_sync_handler (data.bus, (GstBusSyncHandler) bus_sync_handler,NULL);
	//register messages
	gst_bus_add_signal_watch(data.bus);

	//signals we want to connect to
	g_signal_connect(data.bus, "message::error", G_CALLBACK(error_cb),NULL);
	g_signal_connect(data.bus, "message::eos", G_CALLBACK(eos_cb),NULL);
	g_signal_connect(data.bus, "message::state-changed", G_CALLBACK(state_changed_cb),NULL);
	g_signal_connect(data.bus, "message::application", G_CALLBACK(application_cb),NULL);
}

static void record_audio_cb(GtkWidget* widget, GdkEvent * eventt)
{
	disassemble_pipeline();
	data.Mode = RECORD_AUDIO;
	start_audio_recorder();
	//attach_bus_cb();
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static void record_video_cb(GtkWidget* widget, GdkEvent * eventt)
{
	disassemble_pipeline();
	start_video_recorder();
	attach_bus_cb();
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

//when the window gets created the first time, we need to give the xoverlay interface of the pipeline the window handle so it can sink
//into it
static void realize_cb(GtkWidget *widget)
{
	//get window
	GdkWindow * window = gtk_widget_get_window(widget);

	//check window type
	if(!gdk_window_ensure_native(window))
		g_printerr("Not a native window!\n");

	//get handle and attach to pipeline
	//video_window_xid = (gintptr)GDK_WINDOW_HWND (window); I DID STUFF
	video_window_xid = GDK_WINDOW_XID (window);
}

static void audio_encoding_cb(GtkWidget* widget, GdkEvent * eventt)
{
	if(gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON(audio_mulaw))==TRUE)
	{
		data.audio_encoder = MULAW;
	}
	else if(gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON(audio_alaw))==TRUE)
	{
		data.audio_encoder = ALAW;
	}
	else if(gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON(audio_mkv))==TRUE)
	{
		data.audio_encoder = MKV;
	}
}

static void video_encoding_cb(GtkWidget* widget, GdkEvent * eventt)
{
	g_print("GOT HERE\n");
	if(gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON(video_mjpeg))==TRUE)
	{
		g_print("CHANGING TO MJPEG\n");
		data.video_encoder = MJPEG;
	}
	else if(gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON(video_mpeg))==TRUE)
	{
		g_print("CHANGING TO MPEG\n");
		data.video_encoder = MPEG;
	}
}

/* This function is called when the FILE OPEN button is clicked */
static void fileopen_cb (GtkButton *button) 
{
    char *filename;
  	GtkWidget *dialog;
	
	dialog = gtk_file_chooser_dialog_new ("Open File",
				      data.main_window,
				      GTK_FILE_CHOOSER_ACTION_OPEN,
				      GTK_STOCK_CANCEL, GTK_RESPONSE_CANCEL,
				      GTK_STOCK_OPEN, GTK_RESPONSE_ACCEPT,
				      NULL);
	
	if (gtk_dialog_run (GTK_DIALOG (dialog)) == GTK_RESPONSE_ACCEPT)
  	{
        filename = gtk_file_chooser_get_filename (GTK_FILE_CHOOSER (dialog));
        g_print("File path %s \n",filename);
    }
  	data.rate = GST_CLOCK_TIME_NONE;
	gtk_widget_destroy (dialog);
    disassemble_pipeline();
	start_player(filename);

     gtk_widget_set_sensitive(player_controls.play_button, TRUE);
     gtk_widget_set_sensitive(player_controls.pause_button, TRUE);
    gtk_widget_set_sensitive(player_controls.stop_button, TRUE);
    gtk_widget_set_sensitive(player_controls.fileopen_button, TRUE);
    gtk_widget_set_sensitive(player_controls.fastforward_button, TRUE);
    gtk_widget_set_sensitive(player_controls.fastrewind_button, TRUE);

	attach_bus_cb();
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static void fastforward_cb (GtkButton *button) 
{
    data.rate *= 2.0;
    send_seek_event();
}


static void rewind_cb (GtkButton *button) 
{     
    if(data.rate > 0)
        data.rate = -1.0;
    else
        data.rate *= 2.0;
}

static gboolean expose_cb (GtkWidget *widget, GdkEventExpose *event) 
{
    if (data.state < GST_STATE_PAUSED) 
    {
        GtkAllocation allocation;
        GdkWindow *window = gtk_widget_get_window (widget);
        cairo_t *cr;
         
        /* Cairo is a 2D graphics library which we use here to clean the video window.
         * It is used by GStreamer for other reasons, so it will always be available to us. */
        gtk_widget_get_allocation (widget, &allocation);
        cr = gdk_cairo_create (window);
        cairo_set_source_rgb (cr, 0, 0, 0);
        cairo_rectangle (cr, 0, 0, allocation.width, allocation.height);
        cairo_fill (cr);
        cairo_destroy (cr);
    }   
    return FALSE;
}
   
/* This function is called when the slider changes its position. We perform a seek to the
 * new position here. */
static void slider_cb (GtkRange *range) 
{
    gdouble value = gtk_range_get_value (GTK_RANGE (data.slider));
    gst_element_seek_simple (data.pipeline, GST_FORMAT_TIME, GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_KEY_UNIT,
      (gint64)(value * GST_SECOND));
}

static void tags_cb (GstElement *pipeline, gint stream) 
{
    /* We are possibly in a GStreamer working thread, so we notify the main
    * thread of this event through a message in the bus */
    gst_element_post_message (pipeline,
    gst_message_new_application (GST_OBJECT (pipeline),
      gst_structure_new ("tags-changed", NULL)));
}

/* Used by fast forward and fast rewind functions to change advance rate.  */
static void send_seek_event () 
{
    gint64 position;
    GstFormat format = GST_FORMAT_TIME;
    GstEvent *seek_event;
   
    /* Obtain the current position, needed for the seek event */
    if (!gst_element_query_position (data.pipeline, &format, &position)) 
    {
       g_printerr ("Unable to retrieve current position.\n");
       return;
    }
   
    /* Create the seek event */
    if (data.rate > 0) 
    {
        seek_event = gst_event_new_seek (data.rate, GST_FORMAT_TIME, GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE,
        GST_SEEK_TYPE_SET, position, GST_SEEK_TYPE_NONE, 0);
    } else {
        seek_event = gst_event_new_seek (data.rate, GST_FORMAT_TIME, GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE,
        GST_SEEK_TYPE_SET, 0, GST_SEEK_TYPE_SET, position);
    }
   
    if (data.sink2 == NULL) 
    {
        /* If we have not done so, obtain the sink through which we will send the seek events */
        g_object_get (data.pipeline, "video-sink", &data.sink2, NULL);
    }
   
    /* Send the event */
    gst_element_send_event (data.sink2, seek_event);
   
    g_print ("Current rate: %g\n", data.rate);
}

/* Extract metadata from all the streams and write it to the text widget in the GUI */
static void analyze_streams () 
{
    gint i;
    GstTagList *tags;
    gchar *str, *total_str;
    guint rate;
    gint n_video, n_audio, n_text;
    GtkTextBuffer *text;
   
    /* Clean current contents of the widget */
    text = gtk_text_view_get_buffer (GTK_TEXT_VIEW (data.streams_list));
    gtk_text_buffer_set_text (text, "", -1);
   
    /* Read some properties */
    g_object_get (data.pipeline, "n-video", &n_video, NULL);
    g_object_get (data.pipeline, "n-audio", &n_audio, NULL);
    g_object_get (data.pipeline, "n-text", &n_text, NULL);
   
    for (i = 0; i < n_video; i++) 
    {
        tags = NULL;
        /* Retrieve the stream's video tags */
        g_signal_emit_by_name (data.pipeline, "get-video-tags", i, &tags);
        if (tags) 
        {
            total_str = g_strdup_printf ("video stream %d:\n", i);
            gtk_text_buffer_insert_at_cursor (text, total_str, -1);
            g_free (total_str);
            gst_tag_list_get_string (tags, GST_TAG_VIDEO_CODEC, &str);
            total_str = g_strdup_printf ("  codec: %s\n", str ? str : "unknown");
            gtk_text_buffer_insert_at_cursor (text, total_str, -1);
            g_free (total_str);
            g_free (str);
            gst_tag_list_free (tags);
        }
    }
   
    for (i = 0; i < n_audio; i++) 
    {
        tags = NULL;
        /* Retrieve the stream's audio tags */
        g_signal_emit_by_name (data.pipeline, "get-audio-tags", i, &tags);
        if (tags) 
        {
            if (gst_tag_list_get_uint (tags, GST_TAG_TITLE, &str)) 
            {
                total_str = g_strdup_printf ("  Title: %d\n", str);
                gtk_text_buffer_insert_at_cursor (text, total_str, -1);
                g_free (total_str);
            }
            total_str = g_strdup_printf ("\naudio stream %d:\n", i);
            gtk_text_buffer_insert_at_cursor (text, total_str, -1);
            g_free (total_str);
            if (gst_tag_list_get_string (tags, GST_TAG_AUDIO_CODEC, &str)) 
            {
                total_str = g_strdup_printf ("  codec: %s\n", str);
                gtk_text_buffer_insert_at_cursor (text, total_str, -1);
                g_free (total_str);
                g_free (str);
            }
            if (gst_tag_list_get_string (tags, GST_TAG_LANGUAGE_CODE, &str)) 
            {
                total_str = g_strdup_printf ("  language: %s\n", str);
                gtk_text_buffer_insert_at_cursor (text, total_str, -1);
                g_free (total_str);
                g_free (str);
            }
            if (gst_tag_list_get_uint (tags, GST_TAG_BITRATE, &rate)) 
            {
                total_str = g_strdup_printf ("  bitrate: %d\n", rate);
                gtk_text_buffer_insert_at_cursor (text, total_str, -1);
                g_free (total_str);
            }
            gst_tag_list_free (tags);
        }
    }
   
    for (i = 0; i < n_text; i++) 
    {
        tags = NULL;
        /* Retrieve the stream's subtitle tags */
        g_signal_emit_by_name (data.pipeline, "get-text-tags", i, &tags);
        if (tags) 
        {
            total_str = g_strdup_printf ("\nsubtitle stream %d:\n", i);
            gtk_text_buffer_insert_at_cursor (text, total_str, -1);
            g_free (total_str);
            if (gst_tag_list_get_string (tags, GST_TAG_LANGUAGE_CODE, &str)) 
            {
                total_str = g_strdup_printf ("  language: %s\n", str);
                gtk_text_buffer_insert_at_cursor (text, total_str, -1);
                g_free (total_str);
                g_free (str);
            }
            gst_tag_list_free (tags);
        }
    }
}
   
/* This function is called when an "application" message is posted on the bus.
 * Here we retrieve the message posted by the tags_cb callback */
static void application_cb (GstBus *bus, GstMessage *msg) 
{
    if (g_strcmp0 (gst_structure_get_name (msg->structure), "tags-changed") == 0) 
    {
        /* If the message is the "tags-changed" (only one we are currently issuing), update
        * the stream info GUI */
        g_print("tags-changed");
        analyze_streams (data);
    }
}

static gboolean refresh_ui () {
  GstFormat fmt = GST_FORMAT_TIME;
  gint64 current = -1;
   
  /* We do not want to update anything unless we are in the PAUSED or PLAYING states */
  if (data.state < GST_STATE_PAUSED)
    return TRUE;
   
  /* If we didn't know it yet, query the stream duration */
  if (!GST_CLOCK_TIME_IS_VALID (data.duration)) {
    g_print("duration was invalid\n");
    
    if (!gst_element_query_duration (data.pipeline, &fmt, &data.duration)) {
      g_printerr ("Could not query current duration.\n");
    } else {
      /* Set the range of the slider to the clip duration, in SECONDS */
      gtk_range_set_range (GTK_RANGE (data.slider), 0, (gdouble)data.duration / GST_SECOND);
    }
  }
   
  if (gst_element_query_position (data.pipeline, &fmt, &current)) {
    /* Block the "value-changed" signal, so the slider_cb function is not called
     * (which would trigger a seek the user has not requested) */
    g_signal_handler_block (data.slider, data.slider_update_signal_id);
    /* Set the position of the slider to the current pipeline positoin, in SECONDS */
    gtk_range_set_value (GTK_RANGE (data.slider), (gdouble)current / GST_SECOND);
    /* Re-enable the signal */
    g_signal_handler_unblock (data.slider, data.slider_update_signal_id);
  }
  return TRUE;
}
