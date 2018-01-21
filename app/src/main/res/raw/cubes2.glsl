
#ifdef GL_ES
precision highp float;
#endif


// Ray-marching with soft shadows by Omar El Sayyed.
//
// For more information about ray-marching, read:
// http://www.pouet.net/topic.php?which=8177&page=2
// http://www.iquilezles.org/www/articles/raymarchingdf/raymarchingdf.htm
// http://9bitscience.blogspot.com/2013/07/raymarching-distance-fields_14.html
// These should be more than enough to get you started.
//
// Join us on our quest for learning shaders: https://www.facebook.com/groups/748114015233903/
// And please like our page :P
// https://www.facebook.com/nomonesoftware

#define TIME_SCALE 0.7

#define LIGHT_PANNING
#define CAMERA_PANNING
#define CAMERA_ROTATION

uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

float uBoxDistance(vec3 point, float sideLength) {
  return length(max(abs(point)-vec3(sideLength), 0.0));
}

float sSphereDistance(vec3 point, float radius) {
	return length(point) - radius;
}

float sPlaneDistance(vec3 point, vec3 unitPlaneNormal, vec3 pointOnPlane) {
	return dot(point-pointOnPlane, unitPlaneNormal);
}
	
float sceneDistance(vec3 point) {

	float plane = sPlaneDistance(point, vec3(0.0, 1.0, 0.0), vec3(0.0, -0.25, 0.0));
	
	float repeatedShape;
	if (mod(time, 20.0) < 10.0) {
		repeatedShape = sSphereDistance(vec3(mod(point.x, 0.25)-0.125, point.y+0.17, mod(point.z, 0.25)-0.125), 0.08);
	} else {
		repeatedShape = uBoxDistance(vec3(mod(point.x, 0.25)-0.125, point.y+0.19, mod(point.z, 0.25)-0.125), 0.06);
	}
		
	float distance = min(repeatedShape, plane);

	return distance;
}

vec4 checkerBoardTexture(vec2 position);
vec4 sceneTexture(vec3 point) {
	float plane = sPlaneDistance(point, vec3(0.0, 1.0, 0.0), vec3(0.0, -0.25, 0.0));

	float repeatedShape;
	if (mod(time, 20.0) < 10.0) {
		repeatedShape = sSphereDistance(vec3(mod(point.x, 0.25)-0.125, point.y+0.17, mod(point.z, 0.25)-0.125), 0.08);
	} else {
		repeatedShape = uBoxDistance(vec3(mod(point.x, 0.25)-0.125, point.y+0.19, mod(point.z, 0.25)-0.125), 0.06);
	}

	if (repeatedShape < plane) return vec4(1.0);
	
	return checkerBoardTexture(point.xz);
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
	for (int i=0; i<24; i++) {
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

vec4 checkerBoardTexture(vec2 position) {  
  
	position = fract(position);
	if (position.x < 0.5) {
		if (position.y < 0.5) {
			return vec4(1.0, 1.0, 1.0, 1.0);
		} else {
			return vec4(0.2, 0.2, 0.2, 1.0);
		}  
	} else {
		if (position.y < 0.5) {
			return vec4(0.2, 0.2, 0.2, 1.0);
		} else {
			return vec4(1.0, 1.0, 1.0, 1.0);
		}  
	}
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
  	float aboutYAngle = 1.5 * sin(0.7123 * frameTime);  
	float aboutZAngle = 0.8 * sin(frameTime); 
	
	aboutYAngle = mouse.y;
	aboutZAngle = mouse.x;
	 
	
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
	eyePosition.x += 14.0 * sin(0.0123*frameTime);
	eyePosition.z += 14.0 * sin(0.1270*frameTime);
	#endif
	pointPosition += eyePosition;  	

	// Light panning,
	vec3 lightPosition = vec3(0.0, 10.0, 1.0);
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

		// Texture,
		gl_FragColor = sceneTexture(intersectionPoint);
			
		// Light and shadows,
		// Shadows,
		unitRay = normalize(lightPosition - intersectionPoint);
		float softShadow = shadow(unitRay, intersectionPoint + (unitRay*0.01));
		gl_FragColor *= softShadow;
			
		// Light,
		vec3 pointNormal = sceneNormal(intersectionPoint);
		vec4 ambientColor = vec4(0.05, 0.05, 0.05, 1.0);
		vec4 diffuseColor = vec4(0.6, 0.4, 0.4, 0.0);
		vec4 specularColor = vec4(0.5, 0.5, 0.5, 0.0);
		float shininess = 40.0;
		
		gl_FragColor *= light(
			intersectionPoint, 
			pointNormal, 
			lightPosition, 
			eyePosition, 
			vec4(0.0), 
			diffuseColor, 
			specularColor, shininess);

		// Ambient,
		gl_FragColor += ambientColor;
		
		// Fog,
		gl_FragColor = mix(gl_FragColor, fogColor, min(distance-1.0, 3.0)/3.0);
	} else {
		gl_FragColor = fogColor;
	}
	
	// Gamma correction,
	gl_FragColor = pow(gl_FragColor, vec4(1.0/2.2));
}

