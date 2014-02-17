#include "custom_pipeline.cpp"

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
		gst_x_overlay_set_window_handle (xoverlay, video_window_xid);
	} else {
		g_warning ("Should have obtained video_window_xid by now!");
	}

	gst_message_unref (message);
	return GST_BUS_DROP;
}


static void delete_event_cb(GtkWidget * widget, GdkEvent * eventt)
{
	gtk_main_quit(); 
}

static void post_message();

static void record_cb(GtkWidget* widget, GdkEvent * eventt)
{
	disassemble_pipeline();
	assemble_pipeline_with_record();
	data.bus = gst_pipeline_get_bus (GST_PIPELINE (data.pipeline));
	gst_bus_set_sync_handler (data.bus, (GstBusSyncHandler) bus_sync_handler,NULL);
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static void play_cb(GtkWidget* widget, GdkEvent * eventt)
{
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
}

static void pause_cb(GtkWidget* widget, GdkEvent * eventt)
{
	gst_element_set_state(data.pipeline, GST_STATE_PAUSED);
}

static void stop_cb(GtkWidget* widget, GdkEvent * eventt)
{
	disassemble_pipeline();
	assemble_pipeline_without_record();
	data.bus = gst_pipeline_get_bus (GST_PIPELINE (data.pipeline));
	gst_bus_set_sync_handler (data.bus, (GstBusSyncHandler) bus_sync_handler,NULL);
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
	g_free(&debug_info);

	gst_element_set_state(data.pipeline, GST_STATE_READY);
}

static void eos_cb(GstBus * bus, GstMessage * msg)
{
	g_print("Reached End of Stream\n");
	gst_element_set_state(data.pipeline, GST_STATE_READY);
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
	}
	g_print("%d\n", change_request);
	if(change_request)
	{
		g_print("Pipeline modify request\n");
		//handle the state change here. Post an application message to the bus to handle the switch of the pipeline
		gst_element_post_message (data.pipeline, gst_message_new_application (GST_OBJECT (data.pipeline), gst_structure_new ("tags-changed", NULL)));
		//gst_bus_post (bus, gst_message_new_application(GST_OBJECT_CAST (data->pipeline), gst_structure_new_empty ("ExPrerolled")));
		change_request = FALSE;
		//gst_structure_new_empty(
	}
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

	//get handle
	//data->window_handle = window_handle;
	//attach to pipeline
	video_window_xid = (gintptr)GDK_WINDOW_HWND (window);
	//g_print("OUT//Window ID is: %d\n", data->window_handle);
	//g_print("OUT//Window ID is: %d\n", video_window_xid);
}

static void application_cb(GstBus *bus, GstMessage *msg)
{
	g_print("Pipeline modify request QUITTED\n");
	if (g_strcmp0 (gst_structure_get_name (msg->structure), "tags-changed") == 0) 
	{
		gtk_main_quit();
		g_print("Pipeline modify request QUITTED\n");
		//test pipeline switch//////////////////////////////////////////////
		//gst_element_set_state (data->pipeline, GST_STATE_NULL);
		//gst_object_unref(data->pipeline);
		//data->pipeline = gst_pipeline_new("test-pipeline");
		//data->source = gst_element_factory_make("videotestsrc","source");		
		//data->sink = gst_element_factory_make("d3dvideosink", "sink");
		//g_object_set (data->source, "pattern", 0, NULL);
		//gst_bin_add_many(GST_BIN(data->pipeline), data->source, data->sink, NULL);
		//gst_element_link(data->source, data->sink);
		//// set up sync handler for setting the xid once the pipeline is started
		//bus = gst_pipeline_get_bus (GST_PIPELINE (data->pipeline));
		//gst_bus_set_sync_handler (bus, (GstBusSyncHandler) bus_sync_handler, &data);
		//////register messages
		////gst_bus_add_signal_watch(bus);
		//////signals we want to connect to
		////g_signal_connect(bus, "message::error", G_CALLBACK(error_cb),&data);
		////g_signal_connect(bus, "message::eos", G_CALLBACK(eos_cb),&data);
		////g_signal_connect(bus, "message::state-changed", G_CALLBACK(state_changed_cb),&data);
		////g_signal_connect(bus, "message::application", G_CALLBACK(application_cb),&data);
		//gst_object_unref (bus);
		//gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
		//gtk_main();
		//////////////////////////////////////////////////////////////////////		
	}
}
