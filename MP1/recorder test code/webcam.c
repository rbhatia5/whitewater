#include "GTK_UI.c"

int main(int argc, char * argv[])
{
	//declare elements
	GstMessage* msg;
	GstStateChangeReturn ret;

	gst_init(&argc, &argv);
	gtk_init(&argc, &argv);
	memset(&data,0,sizeof(data));
	
	data.Mode = STREAM;
	data.audio_encoder = ALAW;
	create_ui();

	assemble_pipeline();
	attach_bus_cb();
	gst_element_set_state(data.pipeline, GST_STATE_PLAYING);	

	gtk_main();

	disassemble_pipeline();
	
	gst_element_set_state(data.pipeline, GST_STATE_NULL);
	gst_object_unref(data.pipeline);

	return 0;
}