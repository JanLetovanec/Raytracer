package uk.ac.cam.cl.gfxintro.jl2119.tick1;

public class Sphere extends SceneObject {

	// Sphere coefficients
	private final double SPHERE_KD = 0.8;
	private final double SPHERE_KS = 1.2;
	private final double SPHERE_ALPHA = 10;
	private final double SPHERE_REFLECTIVITY = 0.3;

	// The world-space position of the sphere
	private Vector3 position;

	public Vector3 getPosition() {
		return position;
	}

	// The radius of the sphere in world units
	private double radius;

	public Sphere(Vector3 position, double radius, ColorRGB colour) {
		this.position = position;
		this.radius = radius;
		this.colour = colour;

		this.phong_kD = SPHERE_KD;
		this.phong_kS = SPHERE_KS;
		this.phong_alpha = SPHERE_ALPHA;
		this.reflectivity = SPHERE_REFLECTIVITY;
	}

	public Sphere(Vector3 position, double radius, ColorRGB colour, double kD, double kS, double alphaS, double reflectivity) {
		this.position = position;
		this.radius = radius;
		this.colour = colour;

		this.phong_kD = kD;
		this.phong_kS = kS;
		this.phong_alpha = alphaS;
		this.reflectivity = reflectivity;
	}

	/*
	 * Calculate intersection of the sphere with the ray. If the ray starts inside the sphere,
	 * intersection with the surface is also found.
	 */
	public RaycastHit intersectionWith(Ray ray) {

		// Get ray parameters
		Vector3 O = ray.getOrigin();
		Vector3 D = ray.getDirection();
		
		// Get sphere parameters
		Vector3 C = position;
		double r = radius;

		// Calculate quadratic coefficients
		double a = D.dot(D);
		double b = 2 * D.dot(O.subtract(C));
		double c = (O.subtract(C)).dot(O.subtract(C)) - Math.pow(r, 2);

		// Solving quad. eq.
		double determinant = b*b - 4*a*c;
		if (determinant < 0){
            return new RaycastHit();}

		double solution1 = (-b + Math.sqrt(determinant)) / (2 * a);
		double solution2 = (-b - Math.sqrt(determinant)) / (2 * a);
		if(solution1 <= 0 && solution2 <= 0){
		    return new RaycastHit();
        }

		double s;
		if(solution1 >= 0 && solution2 >= 0){
		    s = Math.min(solution1, solution2);
		}
		else {
		    s = Math.max(solution1, solution2);
		}

		Vector3 hitLocation = ray.getDirection().normalised().scale(s);
		hitLocation = ray.getOrigin().add(hitLocation);
		Vector3 hitNormal = this.getNormalAt(hitLocation);
		return new RaycastHit(this, s, hitLocation, hitNormal);
	}

	// Get normal to surface at position
	public Vector3 getNormalAt(Vector3 position) {
		return position.subtract(this.position).normalised();
	}

	// Get
}
