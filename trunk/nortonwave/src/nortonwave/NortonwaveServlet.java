package nortonwave;

import java.util.List;
import java.util.logging.Logger;

import shasta.rating.WRSClient;

import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Element;
import com.google.wave.api.Event;
import com.google.wave.api.Image;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;

@SuppressWarnings("serial")
public class NortonwaveServlet extends AbstractRobotServlet 
{	
	private static final Logger log = Logger.getLogger("NortonwaveServlet");

	private static final String SAFEWAVE_APP_ADDRESS = "nortonwave@appspot.com"; 
	
	// Attention string
	private static final String HAILING_CALL = "Norton:";
	private static final String HELP_TEXT = "My commands are:\n\n\n\tdebug on - Turn debugging on\n\tdebug off - Turn debugging off\n\thelp - Get help\n\n";
	
	// Image names
	private static final String SAFEWAVE_IMAGE_BASE = "http://nortonwave.appspot.com/"; 
	private static final String IMAGE_GOOD = "green-checkmark.png";
	private static final String IMAGE_WARNING = "yellow-checkmark.png";
	private static final String IMAGE_BAD = "red-checkmark.png";
	private static final String IMAGE_UNKNOWN = "grey-checkmark.png";

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
		blip.getDocument().delete();
		
		insertBlipResponseHeading(blip);
			
		if(existingBlip.getText().contains("help"))
		{
			blip.getDocument().append(HELP_TEXT);
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
			blip.getDocument().append(HELP_TEXT);
		}
		return;

	}
		
	//
	// Called each time a blip is added/changed
	//
	private void onBlipSubmitted(Wavelet wavelet, Event e)
	{
		// don't process our own modifications (prevents infinite loop)
		if (e.getModifiedBy().equals(SAFEWAVE_APP_ADDRESS))
		{
			log.info("skipping update from self");
			return;
		}
		
		log.info("processing update from " + e.getModifiedBy());
		
		Blip existingBlip = e.getBlip();
		TextView doc = existingBlip.getDocument();

		// Defect: If the hailing call existing anywhere in the text of a blip, we'll activate... probably want to make sure its the *only* thing there		
		if(doc.getText().contains(HAILING_CALL))
		{
			log.info("processing hailing call");
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
			blip.getDocument().append("Processing blip...\n\n");
		}
		
		// tracks the insertion of each element
		int textOffset = 0;
		
		// rate links
		for(Annotation a : annotationList)
		{
			if(a.getName().contains("link"))
			{
				if(a.getName().contains("manual") || a.getName().contains("auto"))
				{
					// Check if we've already marked it up	
                    Element next = doc.getElement(a.getRange().getEnd() + offset);
                    if(m_bDebug)
						blip.getDocument().append("Image location: " + a.getRange().getEnd() + offset + "\n");
                    	
                    if(next != null)
                    {
                        // Hack Alert - I'm hacking to assume that if a link is followed by an image, then we must have inserted it
                    	// will introduce a new bug which is that we'll skip links if they are followed by images....
                        if(next.isImage())
                        {
                            if(m_bDebug)
        						blip.getDocument().append("Skipping previously marked up link: " + a.getValue() + "\n");
                            
                        	continue;
                        }
                    }
                    
					// If we haven't marked it up -- check the rating and mark up now
					char response = WRSClient.getRatingForSite(a.getValue());
					
					String ratingMsg = "Site report for: " + a.toString() + "-- Site rating:" + response;
					log.info(ratingMsg);
					if(m_bDebug)
						blip.getDocument().append(ratingMsg + "\n");

					Element img = new Image();
					/*
					 * The method returns one character
						g - for good or green site
 						w - for warning or yellow site
 						b - for bad or red site
						u - for unknown site
					 */
					switch(response)
					{
						case 'g':
							img.setProperty("url", SAFEWAVE_IMAGE_BASE + IMAGE_GOOD);						
							break;
						case 'w':
							img.setProperty("url", SAFEWAVE_IMAGE_BASE + IMAGE_WARNING);
							break;
						case 'b':
							img.setProperty("url", SAFEWAVE_IMAGE_BASE + IMAGE_BAD);
							break;
						case 'u':
						default:
							img.setProperty("url", SAFEWAVE_IMAGE_BASE + IMAGE_UNKNOWN);
							break;
					}
					
					// each added link image (and text) requires that we need to offset the start point by 2
                    doc.insertElement(a.getRange().getEnd() + textOffset, img);
                    textOffset++;
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

		StyledText txt = new StyledText("Norton SafeWave\n\n", StyleType.HEADING4);
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