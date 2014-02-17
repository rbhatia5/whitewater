#include "GTK_UI.cpp"

int main(int argc, char * argv[])
{
	//declare elements
	GstMessage* msg;
	GstStateChangeReturn ret;

	gst_init(&argc, &argv);
	gtk_init(&argc, &argv);
	memset(&data,0,sizeof(data));
	
	create_ui();

	assemble_pipeline_without_record();

	// set up sync handler for setting the xid once the pipeline is started
	data.bus = gst_pipeline_get_bus (GST_PIPELINE (data.pipeline));
	gst_bus_set_sync_handler (data.bus, (GstBusSyncHandler) bus_sync_handler,NULL);
	
	//register messages
	gst_bus_add_signal_watch(data.bus);

	//signals we want to connect to
	g_signal_connect(data.bus, "message::error", G_CALLBACK(error_cb),NULL);
	g_signal_connect(data.bus, "message::eos", G_CALLBACK(eos_cb),NULL);
	g_signal_connect(data.bus, "message::state-changed", G_CALLBACK(state_changed_cb),NULL);
	g_signal_connect(data.bus, "message::application", G_CALLBACK(application_cb),NULL);

	gst_element_set_state (data.pipeline, GST_STATE_PLAYING);

	gtk_main();
	
	//bus = gst_element_get_bus (data.pipeline);
	//msg = gst_bus_timed_pop_filtered (bus, 10000 * GST_MSECOND, GST_MESSAGE_EOS);

	//gst_element_set_state(data.pipeline, GST_STATE_PLAYING);
	//bus = gst_element_get_bus (data.pipeline);
	//msg = gst_bus_timed_pop_filtered (bus, 4000 * GST_MSECOND, GST_MESSAGE_EOS);

	gst_element_set_state(data.pipeline, GST_STATE_NULL);
	gst_object_unref(data.pipeline);

	return 0;
}