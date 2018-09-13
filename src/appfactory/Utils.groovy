package appfactory
import java.util.Base64
import java.security.SecureRandom
import java.lang.StringBuilder
import org.csanchez.jenkins.plugins.kubernetes.pipeline.PodTemplateAction

static def base64Decode(input) {
  return new java.lang.String(Base64.getDecoder().decode(input), "UTF-8")
}

static def randomString( int len ) {
  static final String AB = "0123456789abcdefghijklmnopqrstuvwxyz";
  static SecureRandom rnd = new SecureRandom();

  StringBuilder sb = new StringBuilder( len );

  for( int i = 0; i < len; i++ ) {
    sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
  }

  return sb.toString();
}
