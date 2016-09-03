var gulp = require('gulp'),
    gulpWatch = require('gulp-watch'),
    del = require('del'),
    runSequence = require('run-sequence'),
    argv = process.argv;


/**
 * Ionic hooks
 * Add ':before' or ':after' to any Ionic project command name to run the specified
 * tasks before or after the command.
 */
gulp.task('serve:before', ['watch']);
gulp.task('emulate:before', ['build']);
gulp.task('deploy:before', ['build']);
gulp.task('build:before', ['build']);

// we want to 'watch' when livereloading
var shouldWatch = argv.indexOf('-l') > -1 || argv.indexOf('--livereload') > -1;
gulp.task('run:before', [shouldWatch ? 'watch' : 'build']);

/**
 * Ionic Gulp tasks, for more information on each see
 * https://github.com/driftyco/ionic-gulp-tasks
 *
 * Using these will allow you to stay up to date if the default Ionic 2 build
 * changes, but you are of course welcome (and encouraged) to customize your
 * build however you see fit.
 */
var buildBrowserify = require('ionic-gulp-browserify-typescript');
var buildSass = require('ionic-gulp-sass-build');
var copyHTML = require('ionic-gulp-html-copy');
var copyFonts = require('ionic-gulp-fonts-copy');
var copyScripts = require('ionic-gulp-scripts-copy');
var tslint = require('ionic-gulp-tslint');

var isRelease = argv.indexOf('--release') > -1;

gulp.task('watch', ['clean'], function(done){
  runSequence(
    ['sass', 'html', 'fonts', 'scripts', 'extlibs', 'copyLocale'],
    function(){
      gulpWatch('app/**/*.scss', function(){ gulp.start('sass'); });
      gulpWatch('app/**/*.html', function(){ gulp.start('html'); });
      buildBrowserify({ watch: true }).on('end', done);
    }
  );
});

gulp.task('build', ['clean'], function(done){
  runSequence(
    ['sass', 'html', 'fonts', 'scripts', 'extlibs', 'copyLocale'],
    function(){
      buildBrowserify({
        minify: isRelease,
        browserifyOptions: {
          debug: !isRelease
        },
        uglifyOptions: {
          mangle: false
        }
      }).on('end', done);
    }
  );
});
var gulpConcat = require('gulp-concat');
 gulp.task('extlibs', function() {
   return gulp.src(['www/js/3rdparty/jquery.js', 'www/js/3rdparty/ajaxmultiqueue.js', 'www/js/crypto/passphrasegenerator.js', 'js/qrcode.js', 'www/js/3rdparty/i18next.js', 'js/moment-with-locales.min.js', 'js/init.js', 'www/js/nrs.console.js', 'www/js/util/extensions.js', 'www/js/util/locale.js', 'www/js/nrs.settings.js', 'www/js/crypto/3rdparty/jssha256.js', 'www/js/crypto/3rdparty/cryptojs/sha256.js', 'www/js/util/nxtaddress.js', 'www/js/nrs.constants.js', 'www/js/util/converters.js', 'www/js/3rdparty/jsbn.js', 'www/js/3rdparty/jsbn2.js', 'www/js/crypto/curve25519.js', 'www/js/nrs.util.js', 'www/js/nrs.encryption.js', 'www/js/nrs.server.js'])
     .pipe(gulpConcat('nxtlib.js'))
     .pipe(gulp.dest('www/build/js'));
 });

gulp.task('copyLocale', function(){
  gulp.src("www/locales/**/*.json").pipe(gulp.dest("www/build/locales"));
  gulp.src("SkyNxt/user/skynxt.user").pipe(gulp.dest("www/SkyNxt/user/"));
});

gulp.task('sass', buildSass);
gulp.task('html', copyHTML);
gulp.task('fonts', copyFonts);
gulp.task('scripts', copyScripts);
gulp.task('clean', function(){
  return del('www/build');
});
gulp.task('lint', tslint);
