#include "GTK_UI.c"

int main(int argc, char * argv[])
{
	//declare elements
	GstBus * bus;
	GstMessage* msg;
	GstStateChangeReturn ret;

	gst_init(&argc, &argv); // gstreamer initialization
	gtk_init(&argc, &argv);// gtk initialization 
	memset(&data,0,sizeof(data));
	
	data.Mode = STREAM;
	data.audio_encoder = ALAW;
	data.video_encoder = MJPEG;
	create_ui();
	start_streamer();
	attach_bus_cb();
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);	

    g_timeout_add_seconds (10, (GSourceFunc)refresh_ui, &data);

	gtk_main();

	disassemble_pipeline();
	
	gst_element_set_state(data.pipeline, GST_STATE_NULL);
	gst_object_unref(data.pipeline);

	return 0;
}


