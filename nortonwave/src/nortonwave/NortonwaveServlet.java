package nortonwave;

import java.util.List;
import com.google.wave.api.*;

import shasta.rating.*;

@SuppressWarnings("serial")
public class NortonwaveServlet extends AbstractRobotServlet 
{	
	// The name of this wavelet, used to make sure we don't process our own blips
	private String m_sOwnName = "nortonwave@appspot.com";
	
	// Attention string
	private String m_sHailingCall = "Norton:";
	private String m_sHelpText = "My commands are:\n\n\n\tdebug on - Turn debugging on\n\tdebug off - Turn debugging off\n\thelp - Get help\n\n";
	
	// Output debug strings into the blip
	private boolean m_bDebug = false;
	
	private void insertBlipResponseHeading(Blip blip)
	{
		StyledText txt = new StyledText("Norton SafeWave Servlet\n\n", StyleType.HEADING4);
		blip.getDocument().appendStyledText(txt);
		blip.getDocument().append("\n");
	}

	private void onHailingCall(Wavelet wavelet, Event e, TextView existingBlip)
	{
		Blip blip = wavelet.appendBlip();
		TextView doc = blip.getDocument();
		blip.getDocument().delete();
		
		insertBlipResponseHeading(blip);
			
		if(existingBlip.getText().contains("help"))
		{
			blip.getDocument().append(m_sHelpText);
		}
		else if(existingBlip.getText().contains("debug on"))
		{
			m_bDebug = true;
			blip.getDocument().append("OK, Debug is now on\n");
		}
		else if(existingBlip.getText().contains("debug off"))
		{
			m_bDebug = false;
			blip.getDocument().append("OK, Debug is now off\n");
		}
		else
		{
			blip.getDocument().append("I didn't understand what you want...\n\n");
			blip.getDocument().append(m_sHelpText);
		}
		return;

	}
		
	//
	// Called each time a blip is added/changed
	//
	private void onBlipSubmitted(Wavelet wavelet, Event e)
	{
		// don't process our own modifications (prevents infinite loop)
		if(e.getModifiedBy().equals(m_sOwnName))
			return;
		
		Blip existingBlip = e.getBlip();
		TextView doc = existingBlip.getDocument();

		if(doc.getText().contains(m_sHailingCall))
		{
			onHailingCall(wavelet, e, doc);
			return;
		}
				
		Blip blip = null;
		
		List<Annotation> annotationList = doc.getAnnotations();
		
		if(m_bDebug)
		{
			blip = wavelet.appendBlip();
			blip.getDocument().delete();
			insertBlipResponseHeading(blip);
			blip.getDocument().append("Processing blip...");
		}
		int offset = 0;
		
		for(Annotation a : annotationList)
		{
			if(a.getName().contains("link"))
			{
				if(a.getName().contains("manual") || a.getName().contains("auto"))
				{
					char response = WRSClient.getRatingForSite(a.getValue());
					
					if(m_bDebug)
						blip.getDocument().append("Site report for: " + a.toString() + "-- Site rating:" + response + "\n");
										
					Element img = new Image();
					/*
					 * The method returns one character
     					‘ g’ – for good or green site
    				    ‘w’ – for warning or yellow site
      					‘b’ – for bad or red site
      					‘u’ -  for unknown site
					 */
					switch(response)
					{
						case 'g':
							img.setProperty("url", "http://nortonwave.appspot.com/green-checkmark.png");
							break;
						case 'w':
							img.setProperty("url", "http://nortonwave.appspot.com/yellow-checkmark.png");
							break;
						case 'b':
							img.setProperty("url", "http://nortonwave.appspot.com/red-checkmark.png");
							break;
						case 'u':
						default:
							img.setProperty("url", "http://nortonwave.appspot.com/grey-checkmark.png");
							break;
					}
						
					// each added link image requires that we need to offset the start point by 1
                    doc.insertElement(a.getRange().getEnd() + offset, img);
                    offset++;
				}
				else // this would indicate a link to a wave, not an external site:
				{
					if(m_bDebug)
						blip.getDocument().append("Skipping link: " + a.toString() + "\n");
				}
			}
		}		
	}
	
	//
	// What we do when this Robot has been added to a thread.
	//
	private void onSelfAdded(Wavelet wavelet)
	{

		Blip blip = wavelet.appendBlip();
		blip.getDocument().delete();

		StyledText txt = new StyledText("Norton SafeWave Servlet\n\n", StyleType.HEADING4);
		blip.getDocument().appendStyledText(txt);
		blip.getDocument().append("\nType in a link, and I'll tell you if its safe or not\n");
		Element img = new Image();
		img.setProperty("url", "http://nortonwave.appspot.com/iconheader.png");		
		blip.getDocument().appendElement(img);	
	}
	
	//
	// When a new participant is added to the wave
	//
	private void onWaveletParticipantsChanged(Wavelet wavelet, Event e)
	{
		return;
	}
	
	//
	// Main event handler
	//
	@Override
	public void processEvents(RobotMessageBundle bundle) 
	{
		Wavelet wavelet = bundle.getWavelet();
		
		if (bundle.wasSelfAdded())
			onSelfAdded(wavelet);

		for(Event e: bundle.getEvents())
		{
			switch(e.getType())
			{
				case BLIP_SUBMITTED:
					onBlipSubmitted(wavelet, e);
					break;

				case WAVELET_PARTICIPANTS_CHANGED:
					onWaveletParticipantsChanged(wavelet, e);
					break;
			}
		}
	}
}