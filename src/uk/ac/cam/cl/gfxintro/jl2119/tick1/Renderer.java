package uk.ac.cam.cl.gfxintro.jl2119.tick1;

import java.awt.image.BufferedImage;
import java.util.List;

public class Renderer {
	
	// The width and height of the image in pixels
	private int width, height;
	
	// Bias factor for reflected and shadow rays
	private final double EPSILON = 0.0001;

	// The number of times a ray can bounce for reflection
	private int bounces;
	
	// Background colour of the image
	private ColorRGB backgroundColor = new ColorRGB(0.001);

	public Renderer(int width, int height, int bounces) {
		this.width = width;
		this.height = height;
		this.bounces = bounces;
	}

	/*
	 * Trace the ray through the supplied scene, returning the colour to be rendered.
	 * The bouncesLeft parameter is for rendering reflective surfaces.
	 */
	protected ColorRGB trace(Scene scene, Ray ray, int bouncesLeft) {

		// Find closest intersection of ray in the scene
		RaycastHit closestHit = scene.findClosestIntersection(ray);

        // If no object has been hit, return a background colour
        SceneObject object = closestHit.getObjectHit();
        if (object == null){
            return backgroundColor;
        }
        
        // Otherwise calculate colour at intersection and return
        // Get properties of surface at intersection - location, surface normal
        Vector3 P = closestHit.getLocation();
        Vector3 N = closestHit.getNormal();
        Vector3 O = ray.getOrigin();

     	// Solve reflectivity

     	ColorRGB directIllumination =  this.illuminate(scene, object, P, N, O);
        double reflectivity = object.getReflectivity();

        //Base case
        if (bouncesLeft == 0 || reflectivity == 0) {
            return directIllumination;
        }
        ColorRGB reflectedIllumination;

        // Calculate reflected direction R
        // if direction is 0 or normal is, something is wrong
        Vector3 R;
        if (N.magnitude() == 0 || ray.getDirection().magnitude() == 0) {
            R = new Vector3(0);
        } else {
            R = ray.getDirection().normalised().scale(-1).reflectIn(N.normalised());
            R = R.normalised();
        }

        // spawn a biased ray (biased towards the R)
        Vector3 newOrigin = closestHit.getLocation().add(R.scale(EPSILON));
        Ray reflectedRay = new Ray(newOrigin, R);
        // Recursive calls
        reflectedIllumination = this.trace(scene, reflectedRay, bouncesLeft - 1);

        // Mix reflections altogether
        directIllumination = directIllumination.scale(1.0 - reflectivity);
        reflectedIllumination = reflectedIllumination.scale(reflectivity);
        return directIllumination.add(reflectedIllumination);

	}

	/*
	 * Illuminate a surface on and object in the scene at a given position P and surface normal N,
	 * relative to ray originating at O
	 */
	private ColorRGB illuminate(Scene scene, SceneObject object, Vector3 P, Vector3 N, Vector3 O) {
	   
		ColorRGB colourToReturn = new ColorRGB(0);

		ColorRGB I_a = scene.getAmbientLighting(); // Ambient illumination intensity

		ColorRGB C_diff = object.getColour(); // Diffuse colour defined by the object
		
		// Get Phong coefficients
		double k_d = object.getPhong_kD();
		double k_s = object.getPhong_kS();
		double alpha = object.getPhong_alpha();

		// Start with ambient
		colourToReturn = colourToReturn.add(C_diff.scale(I_a));

		// Loop over each point light source
		List<PointLight> pointLights = scene.getPointLights();
		for (int i = 0; i < pointLights.size(); i++) {
			PointLight light = pointLights.get(i); // Select point light
			
			// Calculate point light constants
			double distanceToLight = light.getPosition().subtract(P).magnitude();
			ColorRGB C_spec = light.getColour();
			ColorRGB I = light.getIlluminationAt(distanceToLight);

			//Calc. VLR (try not to rely on P, N being unit vectors)
            Vector3 L = light.getPosition().subtract(P);
            if (L.magnitude()!= 0){
                L = L.normalised();
            }
            Vector3 V = O.subtract(P);
            if (V.magnitude() != 0) {
                V = V.normalised();
            }
            // If L is unit vector => R is unit vector
            Vector3 R = L.reflectIn(N.normalised());

			// Calculate ColorRGB diffuse
            double dotTerm = Math.max(0, N.normalised().dot(L));
            ColorRGB diffuse = C_diff.scale(k_d).scale(I).scale(dotTerm);
            // Specular
            dotTerm = Math.pow(Math.max(0, R.dot(V)), alpha);
            ColorRGB specular = C_spec.scale(k_s).scale(I).scale(dotTerm);

            // Shadow rays
            Ray shadowRay = new Ray(P.add(L.scale(EPSILON)), L);
            RaycastHit hit = scene.findClosestIntersection(shadowRay);
            if (hit.getDistance() <= distanceToLight) {
                diffuse = new ColorRGB(0);
                specular = new ColorRGB(0);
            }

            colourToReturn = colourToReturn.add(diffuse).add(specular);
		}
		return colourToReturn;
	}

	// Render image from scene, with camera at origin
	public BufferedImage render(Scene scene) {
		
		// Set up image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		// Set up camera
		Camera camera = new Camera(width, height);

		// Loop over all pixels
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Ray ray = camera.castRay(x, y); // Cast ray through pixel
				ColorRGB linearRGB = trace(scene, ray, bounces); // Trace path of cast ray and determine colour
				ColorRGB gammaRGB = tonemap( linearRGB );
				image.setRGB(x, y, gammaRGB.toRGB()); // Set image colour to traced colour
			}
			// Display progress every 10 lines
            if( y % 10 == 0 )
			    System.out.println(String.format("%.2f", 100 * y / (float) (height - 1)) + "% completed");
		}
		return image;
	}


	// Combined tone mapping and display encoding
	public ColorRGB tonemap( ColorRGB linearRGB ) {
		double invGamma = 1./2.2;
		double a = 2;  // controls brightness
		double b = 1.3; // controls contrast

		// Sigmoidal tone mapping
		ColorRGB powRGB = linearRGB.power(b);
		ColorRGB displayRGB = powRGB.scale( powRGB.add(Math.pow(0.5/a,b)).inv() );

		// Display encoding - gamma
		ColorRGB gammaRGB = displayRGB.power( invGamma );

		return gammaRGB;
	}


}