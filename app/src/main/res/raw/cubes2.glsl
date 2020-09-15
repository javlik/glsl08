#ifdef GL_ES
precision highp float;
#endif

#define TIME_SCALE 0.1

#define LIGHT_PANNING
#define CAMERA_PANNING
#define CAMERA_ROTATION

uniform float time;
uniform vec2 resolution;
//uniform mat4 eye;

vec4 dC;

void pR(inout vec2 p, float a) { 	
  p = cos(a)*p + sin(a)*vec2(p.y, -p.x); 
}

float uBoxDistance(vec3 point, float sideLength) {
  return length(max(abs(point)-vec3(sideLength), 0.) +.0) - .01;
}

float sSphereDistance(vec3 point, float radius) {
	return length(point) - radius;
}

float sPlaneDistance(vec3 point, vec3 unitPlaneNormal, vec3 pointOnPlane) {
 return 100000.;
	return dot(point-pointOnPlane, unitPlaneNormal);
}
	
float sceneDistance(vec3 point) {
	
	float repeatedShape;
	if (false && mod(time, 20.0) < 10.0) {
		repeatedShape = sSphereDistance(vec3(mod(point.x, 0.5)-0.25, mod(point.y, 0.5)-0.25, mod(point.z, 0.5)-0.25), 0.08);
	} else {
      //rX(point,time*.1);
		vec3 bx = vec3(mod(point.x, 0.4)-0.2, mod(point.y, 0.4)-0.2, mod(point.z, 0.4)-0.2);
  float rot=floor(point.x/.4);
  float bx2 = mod(rot,0.4);
  if (bx2>0.0){
     dC = vec4(0.0, 0.3, 0.99, 0.0);
  } else {
     dC = vec4(0.99, 0.99, 0.2, 0.0);
  }
  pR(bx.xz, time);
  pR(bx.yx, time*rot*.1);
  
  repeatedShape = uBoxDistance(bx, 0.06);

	}
		
	//float distance = min(repeatedShape, plane);
 float distance = repeatedShape;

	return distance;
}



vec3 sceneNormal(vec3 point) {
	
	// Partial differentiation to get normal,
	float delta = 0.5 / resolution.x;  // Half a pixel.
	vec3 distanceAtDelta = vec3(
		sceneDistance(point + vec3(delta,   0.0,   0.0)),
		sceneDistance(point + vec3(  0.0, delta,   0.0)),
		sceneDistance(point + vec3(  0.0,   0.0, delta)));
	vec3 normal = distanceAtDelta - vec3(sceneDistance(point));
	
	return normalize(normal);
}

float rayMarch(vec3 unitRay, vec3 raySource) {

	float totalDistance = 0.0;
	for (int i=0; i<64; i++) {
		float distance = sceneDistance(raySource + (unitRay * totalDistance));
		totalDistance += distance; 
		if (distance < 0.001) return totalDistance;
	}
	
	return 0.0;
}

float shadow(vec3 unitRay, vec3 raySource) {

	float totalDistance = 0.0;
	float softShadow = 1.0;
	for (int i=0; i<64; i++) {
		float distance = sceneDistance(raySource + (unitRay * totalDistance));
		totalDistance += distance; 
		softShadow = min(softShadow, 4.0*distance/totalDistance);
		if (distance < 0.001) {
			return 0.0;
		}
	}
	
	return softShadow;
}

vec4 light(vec3 pointPosition, vec3 pointNormal, vec3 lightPosition, vec3 eyePosition, vec4 ambientColor, vec4 diffuseColor, vec4 specularColor, float shininess) {  

	vec3 L = normalize(lightPosition - pointPosition);   
	vec3 E = normalize(eyePosition - pointPosition); 
	vec3 R = normalize(-reflect(L, pointNormal));  
	
	// Calculate ambient term,
	vec4 ambient = ambientColor;
	
	// Calculate diffuse term,
	vec4 diffuse = diffuseColor * max(dot(pointNormal,L), 0.0);
	diffuse = clamp(diffuse, 0.0, 1.0);     
	
	// Calculate specular term,
	vec4 specular = specularColor * pow(max(dot(R,E),0.0),0.3*shininess);
	specular = clamp(specular, 0.0, 1.0); 
	
	return ambient + diffuse + specular;
}

	       
void main(void) {	

	// Camera,
	//vec3 eyePosition = vec3(0.0, 0.0, -1.0);
	//vec3 pointPosition = vec3((2.0 * gl_FragCoord.xy / resolution) - vec2(1.0), 1.0);
	//pointPosition.x *= resolution.x / resolution.y;
	
	vec3 eyePosition = vec3(0.0, 0.5, 0.0);
	vec3 pointPosition = vec3((2.0 * gl_FragCoord.xy / resolution) - vec2(1.0), 1.0);
	pointPosition.x *= resolution.x / resolution.y;
	pointPosition.z = pointPosition.y;
	pointPosition.y = -1.5;

	// Prepare for transformation,
	pointPosition -= eyePosition;
		
	float frameTime = time * TIME_SCALE;
	
	#ifdef CAMERA_ROTATION
	// Camera rotation,
  	//float aboutYAngle = 1.5 * sin(0.7123 * frameTime);  
	//float aboutZAngle = 0.8 * sin(frameTime);  
	
 float aboutYAngle = 0.1 * time;
 float aboutZAngle = 0.05 * time;


	// Rotate around y-axis,  
	vec3 tempPoint = pointPosition;  
	float cosAngle = cos(aboutYAngle);  
	float sinAngle = sin(aboutYAngle);  
	pointPosition.x = (tempPoint.x * cosAngle) - (tempPoint.z * sinAngle);  
	pointPosition.z = (tempPoint.z * cosAngle) + (tempPoint.x * sinAngle);  
	
	// Rotate around z-axis,  
	tempPoint = pointPosition;  
	cosAngle = cos(aboutZAngle);  
	sinAngle = sin(aboutZAngle);  
	pointPosition.x = (tempPoint.x * cosAngle) - (tempPoint.y * sinAngle);  
	pointPosition.y = (tempPoint.y * cosAngle) + (tempPoint.x * sinAngle);  
	#endif
	
	// Panning,
	#ifdef CAMERA_PANNING
	//eyePosition.x += 14.0 * sin(0.0123*frameTime);
	//eyePosition.z += 14.0 * sin(0.1270*frameTime);
// eyePosition.x += -10. * cos(aboutZAngle);
// eyePosition.y += -10. * sin(aboutZAngle);
 //eyePosition.z += -1. * sin(aboutZAngle);
 eyePosition.y -= time/2.;
	#endif
	pointPosition += eyePosition;  	

	// Light panning,
	vec3 lightPosition = vec3(0.0, 10.0, 1.0);
 //vec3 lightPosition = eyePosition;
	#ifdef LIGHT_PANNING
	lightPosition.x += 14.0 * sin(0.4123*frameTime);
	lightPosition.y += 6.0 * sin(0.7234*frameTime);
	lightPosition.z += 14.0 * sin(0.3270*frameTime);
	#endif	
	
	vec3 unitRay = normalize(pointPosition - eyePosition);
	vec4 fogColor = vec4(0.44, 0.64, 1.0, 1.0);

	float distance = rayMarch(unitRay, eyePosition);


	if (distance > 0.0) {
		
		vec3 intersectionPoint = eyePosition + (unitRay * distance);
			
		// Light,
		vec3 pointNormal = sceneNormal(intersectionPoint);
		vec4 ambientColor = vec4(0.05, 0.05, 0.05, 1.0);
		vec4 diffuseColor = vec4(0.2, 0.3, 0.8, 0.0);
		vec4 specularColor = vec4(0.9, 0.9, 0.9, 0.0);
		float shininess = 40.0;
		
  lightPosition = eyePosition;

		gl_FragColor = light(
			intersectionPoint, 
			pointNormal, 
			lightPosition, 
			eyePosition, 
			vec4(0.0), 
			//diffuseColor, 
   dC,
			specularColor, shininess);


      


		// Ambient,
		gl_FragColor += ambientColor;
		


// // Fog,
		gl_FragColor = mix(gl_FragColor, fogColor, min(distance-1.0, 3.0)/3.0);
	} else {
		gl_FragColor = fogColor;
	}
	
	// Gamma correction,
	gl_FragColor = pow(gl_FragColor, vec4(1.0/2.2));

if (distance < .2 && distance >0.){
   gl_FragColor.r = mix(gl_FragColor.r, 1., 10.*(.2-distance));
   }		
}

