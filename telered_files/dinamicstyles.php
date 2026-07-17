
/* reset */
/*
======================================================================================
Reset
Updated: Feb, 2015
Based on normalize.css v3.0.2 | MIT License | http://necolas.github.io/normalize.css/
Author: Juan Gagliardo
======================================================================================
Estandariza la renderización de elementos HTML
*/

html {
	font-family: sans-serif;
	-webkit-text-size-adjust: 100%;
	-ms-text-size-adjust: 100%;
}

body {
	margin: 0;
}

article,aside,details,figcaption,figure,footer,header,hgroup,main,menu,nav,section,summary {
	display: block;
}

audio,canvas,progress,video {
	display: inline-block;
	vertical-align: baseline;
}

a {
	background-color: transparent;
}

a:active,a:hover {
	outline: 0;
}

b,strong {
	font-weight: 700;
}


h1 {
	margin: .67em 0;
	font-size: 2em;
}

mark {
	color: #000;
	background: #ff0;
}

small {
	font-size: 80%;
}

sub,sup {
	position: relative;
	font-size: 75%;
	line-height: 0;
	vertical-align: baseline;
}

sup {
	top: -.5em;
}

sub {
	bottom: -.25em;
}

img {
	border: 0;
}

svg:not(:root) {
	overflow: hidden;
}

figure {
	margin: 1em 40px;
}

hr {
	height: 0;
	-webkit-box-sizing: content-box;
	-moz-box-sizing: content-box;
	box-sizing: content-box;
}

pre {
	overflow: auto;
}

code,kbd,pre,samp {
	font-family: monospace,monospace;
	font-size: 1em;
}

button,input,optgroup,select,textarea {
	margin: 0;
	font: inherit;
	color: inherit;
}

button {
	overflow: visible;
}

button,select {
	text-transform: none;
}

button,html input[type=button],input[type=reset],input[type=submit] {
	-webkit-appearance: button;
	cursor: pointer;
}

button[disabled],html input[disabled] {
	cursor: default;
}

button::-moz-focus-inner,input::-moz-focus-inner {
	padding: 0;
	border: 0;
}

input {
	line-height: normal;
}

input[type=checkbox],input[type=radio] {
	-webkit-box-sizing: border-box;
	-moz-box-sizing: border-box;
	box-sizing: border-box;
	padding: 0;
}

input[type=number]::-webkit-inner-spin-button,input[type=number]::-webkit-outer-spin-button {
	height: auto;
}

input[type=search] {
	-webkit-box-sizing: content-box;
	-moz-box-sizing: content-box;
	box-sizing: content-box;
	-webkit-appearance: textfield;
}

input[type=search]::-webkit-search-cancel-button,input[type=search]::-webkit-search-decoration {
	-webkit-appearance: none;
}

fieldset {
	padding: .35em .625em .75em;
	margin: 0 2px;
	border: 1px solid silver;
}

legend {
	padding: 0;
	border: 0;
}

textarea {
	overflow: auto;
}

optgroup {
	font-weight: 700;
}

table {
	border-spacing: 0;
	border-collapse: collapse;
}

td,th {
	padding: 0;
}

/*! Source: https: //github.com/h5bp/html5-boilerplate/blob/master/src/css/main.css */
@media print {
	*,:before,:after {
		color: #000!important;
		text-shadow: none!important;
		background: transparent!important;
		-webkit-box-shadow: none!important;
		box-shadow: none!important;
	}
	
	a,a:visited {
		text-decoration: underline;
	}
	
	a[href]:after {
		content: " (" attr(href) ")";
	}
	
	abbr[title]:after {
		content: " (" attr(title) ")";
	}
	
	a[href^="#"]:after,a[href^="javascript: "]:after {
		content: "";
	}
	
	pre,blockquote {
		border: 1px solid #999;
		page-break-inside: avoid;
	}
	
	thead {
		display: table-header-group;
	}
	
	tr,img {
		page-break-inside: avoid;
	}
	
	img {
		max-width: 100%!important;
	}
	
	p,h2,h3 {
		orphans: 3;
		widows: 3;
	}
	
	h2,h3 {
		page-break-after: avoid;
	}
	
	select {
		background: #fff!important;
	}
}

* {
	-webkit-box-sizing: border-box;
	-moz-box-sizing: border-box;
	box-sizing: border-box;
}

:before,:after {
	-webkit-box-sizing: border-box;
	-moz-box-sizing: border-box;
	box-sizing: border-box;
}

html {
	font-size: 10px;
	-webkit-tap-highlight-color: rgba(0,0,0,0);
}

body {
	font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;
	font-size: 14px;
	line-height: 1.42857143;
	color: #333;
	background-color: #fff;
}

input,button,select,textarea {
	font-family: inherit;
	font-size: inherit;
	line-height: inherit;
}

a {
	color: #337ab7;
	text-decoration: none;
}

a:hover,a:focus {
	color: #23527c;
	text-decoration: underline;
}

a:focus {
	outline: none;
}

figure {
	margin: 0;
}

img {
	vertical-align: middle;
}

hr {
	margin-top: 20px;
	margin-bottom: 20px;
	border: 0;
	border-top: 1px solid #eee;
}

h1,h2,h3,h4,h5,h6,.h1,.h2,.h3,.h4,.h5,.h6 {
	font-family: inherit;
	font-weight: 500;
	line-height: 1.1;
	color: inherit;
}

h1 small,h2 small,h3 small,h4 small,h5 small,h6 small,.h1 small,.h2 small,.h3 small,.h4 small,.h5 small,.h6 small,h1 .small,h2 .small,h3 .small,h4 .small,h5 .small,h6 .small,.h1 .small,.h2 .small,.h3 .small,.h4 .small,.h5 .small,.h6 .small {
	font-weight: 400;
	line-height: 1;
	color: #777;
}

h1,.h1,h2,.h2,h3,.h3 {
	margin-top: 20px;
	margin-bottom: 10px;
}

h1 small,.h1 small,h2 small,.h2 small,h3 small,.h3 small,h1 .small,.h1 .small,h2 .small,.h2 .small,h3 .small,.h3 .small {
	font-size: 65%;
}

h4,.h4,h5,.h5,h6,.h6 {
	margin-top: 10px;
	margin-bottom: 10px;
}

h4 small,.h4 small,h5 small,.h5 small,h6 small,.h6 small,h4 .small,.h4 .small,h5 .small,.h5 .small,h6 .small,.h6 .small {
	font-size: 75%;
}

h1,.h1 {
	font-size: 36px;
}

h2,.h2 {
	font-size: 30px;
}

h3,.h3 {
	font-size: 24px;
}

h4,.h4 {
	font-size: 18px;
}

h5,.h5 {
	font-size: 14px;
}

h6,.h6 {
	font-size: 12px;
}

p {
	margin: 0 0 10px;
}


small,.small {
	font-size: 85%;
}

mark,.mark {
	padding: .2em;
	background-color: #fcf8e3;
}

ul,ol {
	margin-top: 0;
	margin-bottom: 10px;
}

ul ul,ol ul,ul ol,ol ol {
	margin-bottom: 0;
}

dl {
	margin-top: 0;
	margin-bottom: 20px;
}

dt,dd {
	line-height: 1.42857143;
}

dt {
	font-weight: 700;
}

dd {
	margin-left: 0;
}

abbr[title],abbr[data-original-title] {
	cursor: help;
	border-bottom: 1px dotted #777;
}

blockquote {
	padding: 10px 20px;
	margin: 0 0 20px;
	font-size: 17.5px;
	border-left: 5px solid #eee;
}

blockquote p:last-child,blockquote ul:last-child,blockquote ol:last-child {
	margin-bottom: 0;
}

blockquote footer,blockquote small,blockquote .small {
	display: block;
	font-size: 80%;
	line-height: 1.42857143;
	color: #777;
}

blockquote footer:before,blockquote small:before,blockquote .small:before {
	content: '\2014 \00A0';
}


address {
	margin-bottom: 20px;
	font-style: normal;
	line-height: 1.42857143;
}

code,kbd,pre,samp {
	font-family: Menlo,Monaco,Consolas,"Courier New",monospace;
}

code {
	padding: 2px 4px;
	font-size: 90%;
	color: #c7254e;
	background-color: #f9f2f4;
	border-radius: 4px;
}

kbd {
	padding: 2px 4px;
	font-size: 90%;
	color: #fff;
	background-color: #333;
	border-radius: 3px;
	-webkit-box-shadow: inset 0 -1px 0 rgba(0,0,0,.25);
	box-shadow: inset 0 -1px 0 rgba(0,0,0,.25);
}

kbd kbd {
	padding: 0;
	font-size: 100%;
	font-weight: 700;
	-webkit-box-shadow: none;
	box-shadow: none;
}

pre {
	display: block;
	padding: 9.5px;
	margin: 0 0 10px;
	font-size: 13px;
	line-height: 1.42857143;
	color: #333;
	word-break: break-all;
	word-wrap: break-word;
	background-color: #f5f5f5;
	border: 1px solid #ccc;
	border-radius: 4px;
}

pre code {
	padding: 0;
	font-size: inherit;
	color: inherit;
	white-space: pre-wrap;
	background-color: transparent;
	border-radius: 0;
}

table {
	background-color: transparent;
}

caption {
	padding-top: 8px;
	padding-bottom: 8px;
	color: #777;
	text-align: left;
}

th {
	text-align: left;
}

fieldset {
	min-width: 0;
	padding: 0;
	margin: 0;
	border: 0;
}

legend {
	display: block;
	width: 100%;
	padding: 0;
	margin-bottom: 20px;
	font-size: 21px;
	line-height: inherit;
	color: #333;
	border: 0;
	border-bottom: 1px solid #e5e5e5;
}

label {
	display: inline-block;
	max-width: 100%;
	margin-bottom: 5px;
	font-weight: 700;
}

input[type=search] {
	-webkit-box-sizing: border-box;
	-moz-box-sizing: border-box;
	box-sizing: border-box;
}

input[type=radio],input[type=checkbox] {
	margin: 4px 0 0;
	margin-top: 1px;
	line-height: normal;
}

input[type=file] {
	display: block;
}

input[type=range] {
	display: block;
	width: 100%;
}

select[multiple],select[size] {
	height: auto;
}

select::-ms-expand {
	display: none;
}

input[type=file]:focus,input[type=radio]:focus,input[type=checkbox]:focus {
	outline: none;
}

output {
	display: block;
	padding-top: 7px;
	font-size: 14px;
	line-height: 1.42857143;
	color: #555;
}

input[type=search] {
	-webkit-appearance: none;
}

@media screen and (-webkit-min-device-pixel-ratio: 0) {
	input[type=date],input[type=time],input[type=datetime-local],input[type=month] {
		line-height: 34px;
	}
}

input[type=radio][disabled],input[type=checkbox][disabled],input[type=radio].disabled,input[type=checkbox].disabled,fieldset[disabled] input[type=radio],fieldset[disabled] input[type=checkbox] {
	cursor: not-allowed;
}

.nav {
	padding-left: 0;
	margin-bottom: 0;
	list-style: none;
}

.nav>li {
	position: relative;
	display: block;
}

.nav>li>a {
	position: relative;
	display: block;
	padding: 10px 15px;
}

.nav>li>a:hover,.nav>li>a:focus {
	text-decoration: none;
	background-color: #eee;
}

.nav>li>a>img {
	max-width: none;
}

@-ms-viewport {
	width: device-width;
}/* END reset */

/* global */
/* Estilos básicos para todas las páginas */

/* Modal */
#modalwrapper {
position: fixed;
display: none;
top: 0;
left: 0;
width: 100%;
height: 100%;
background: rgba(0, 0, 0, 0.83);
z-index: 999999;
text-align: center;
/*padding: 10px;*/
padding: 50px 10px 10px 10px;
}

#modalwrapper.landscape {
padding: 10px 50px;
}

#modalcontentwrapper {
position: relative;
display: inline-block;
max-width: 100%;
margin: 0 auto;
background: transparent;
text-align: left;
visibility: hidden;
}

#modalcontent {
background: white;
border: none;
border-radius: 0;
margin: 0 auto;
padding: 0;
display: block;
position: relative;
z-index: 9999999;
overflow-x: hidden;
overflow-y: auto;
}

#modalcontent.imgmodal {
display: -moz-inline-stack; /* Corrige bug de FF: el ancho de display:block no se ajusta al contenido display:inline-block */
}

#modalcontent.framed {
border: 1px solid silver;
/*border-radius: 10px;*/
padding: 12px;
}

#modalcontent.framed.titled {
border-top: 0;
/*border-radius: 0 0 10px 10px;*/
}

#modalcontent a {
text-decoration: none;
}

#modalcontent img {
}

#modalheader {
background: white;
display: block;
min-height: 50px;
margin: 0 auto;
padding: 12px 42px 12px 12px;
position: relative;
border: 1px solid silver;
border-bottom: 0;
/*border-radius: 10px 10px 0 0;*/
}

#modalheader .pagetitle {
padding: 0;
}

#modalheader, #modalcontent {
max-width: 90%;
max-width: 100%;
}

#closemodal, .closemodal {
width: 22px;
height: 22px;
box-sizing: content-box;
/*background: rgba(255, 255, 255, 0.53);*/
background: transparent;
display: block;
position: absolute;
/*top: 10px;*/
top: -40px;
/*left: -6px;*/
right: -7px;
cursor: pointer;
z-index: 10000000;
border-radius: 50%;
overflow: hidden;
border: 4px solid transparent;
}

@media (max-width: 699px), (orientation: portrait) {
#closemodal, .closemodal {
right: calc(100% - 23px);
position: absolute;
}
}

#modalwrapper.landscape #closemodal, #modalwrapper.landscape .closemodal {
top: -5px;
right: -40px;
}

.closemodalcross {
/*background: #2A2A2A;*/
background: #EAEAEA;
display: block;
width: 40px;
height: 2px;
position: absolute;
top: 10px;
left: -9px;
}

.closemodalcross.top {
-webkit-transform: rotate(45deg);
-ms-transform: rotate(45deg);
transform: rotate(45deg);
}

.closemodalcross.bottom {
-webkit-transform: rotate(-45deg);
-ms-transform: rotate(-45deg);
transform: rotate(-45deg);
}

.modalsubtit {
width: 100%;
display: block;
padding: 12px;
}

.modalsubtit.respuestaopcion {
padding: 0 0 0 12px;
}

.pedido {
padding: 0;
margin: 0;
}

h3.pedido {
font-size: 18px;
}

/* animación mientras carga el contenido */
.loading {
position: fixed;
display: block;
width: 40px;
height: 40px;
background: url(../img/loading.png) no-repeat center center;
background-size: cover;
border-radius: 50%;
padding: 0;
margin: 0;
top: 50%;
top: calc(50% - 20px);
left: 50%;
left: calc(50% - 20px);
visibility: hidden;
}

.searcherblockinner .loading {
position: relative;
}

.sliderblock .loading {
margin: 15px 0 -15px 0;
}

.loading.loaded {
visibility: visible;
-webkit-animation: loading 1s infinite;
-webkit-animation-timing-function: linear;
animation: loading 1s infinite;
animation-timing-function: linear;
}

@-webkit-keyframes loading {
from {
-webkit-transform: rotate(0deg);
-ms-transform: rotate(0deg);
transform: rotate(0deg);
}
to {
transform: rotate(360deg);
-ms-transform: rotate(360deg);
transform: rotate(360deg);
}
}

@keyframes loading {
from {
-webkit-transform: rotate(0deg);
-ms-transform: rotate(0deg);
transform: rotate(0deg);
}
to {
transform: rotate(360deg);
-ms-transform: rotate(360deg);
transform: rotate(360deg);
}
}

/* END Modal */

body {
padding: 0;
margin: 0;
font-family: Open Sans Condensed, sans-serif;
overflow-y: scroll;
}

@media (max-width: 699px), (orientation: portrait) {
body {
padding-top: 46px;
-moz-transition: -moz-transform .25s ease;
-webkit-transition: -webkit-transform .25s ease;
-o-transition: -o-transform .25s ease;
transition: transform .25s ease;
width: 100%;
overflow-x: hidden;
}
}

* {
font: inherit;
}

*:focus {
outline: none;
}

img {
max-width: 100%;
display: block;
margin: 0 auto;
}

.hide {
display: none;
visibility: hidden;
opacity: 0;
width: 0;
}

/* Table styles */
.tablestyle {
display: table;
}

.tablerow {
display: table-row;
}

.tablecell {
display: table-cell;
}
/* END Table styles */

.xxxbutton {
display: inline-block;
padding: 6px 12px;
margin-bottom: 0;
font-size: 14px;
font-weight: 400;
line-height: 1.42857143;
text-align: center;
white-space: nowrap;
vertical-align: middle;
-ms-touch-action: manipulation;
touch-action: manipulation;
cursor: pointer;
-webkit-user-select: none;
-moz-user-select: none;
-ms-user-select: none;
user-select: none;
background-image: none;
border: 1px solid transparent;
border-radius: 4px;
}

.xxxbuttonget, .xxxbuttonget:active, .xxxbuttonget:hover, .xxxbuttonget:focus, .xxxbuttonget:visited {
border-radius: 5px;
/*background-color: #662D91;*/
background-color: #FF9800;
color: white;
border: none;
font-weight: bold;
text-transform: uppercase;
font-size: 21px;
}

.table .xxxbuttonget, .table .xxxbuttonget:hover, .table .xxxbuttonget:active {
font-size: inherit;
padding: 2px 12px;
color: white;
text-decoration: none;
}

.xxxbuttonget:hover {
/*background: #7C37B0;*/
background: #FFA623;
}

.xxxbuttonget:active {
/*background: #421762;*/
background: #EA8400;
}

@media (max-width: 525px) {
.xxxbuttonget, .xxxbuttonget:hover, .xxxbuttonget:active {
font-size: 1em;
}
}

#contentwrapper {
width: 100%;
z-index: -2;
}

@media (max-width: 699px) {
#contentwrapper {
z-index: 0;
}
}

#maincontent {
padding-top: 6px;
position: relative;
}

.section {
width: 100%;
padding: 0;
margin: 0;
display: block;
}

.blockcontainer {
display: block;
max-width: 1080px;
margin: 0 auto;
position: relative;
padding: 12px 60px;
}

@media (max-width: 800px) {
.blockcontainer {
padding: 12px;
}
}

.blockcontainer.maintitle {
padding-bottom: 0px;
}

/* Títulos */
h1, .h1, h2, .h2, h3, .h3, h4, .h4 {
color: #005A96;
padding: .5em 0;
}

.pagetitle {
padding: 6px 0 14px 0;
}

.section.titlesection .pagetitle {
padding-top: 20px;
padding-bottom: 2px;
}

.pagetitle .h1, .pagetitle h1, .pagetitle .h2, .pagetitle h2, .pagetitle .h3, .pagetitle h3, .pagetitle .h4, .pagetitle h4 {
text-transform: uppercase;
display: inline-block;
padding: 0;
margin: 0 .5rem 0 0;
font-weight: normal;
}

.pagetitle h1 {
font-weight: bold;
}

.section .pagetitle h1 {
font-size: 30px;
}

.section .pagetitle h2 {
font-size: 30px;
}

.section.titlesection .pagetitle h1 {
font-size: 36px;
}

@media (max-width: 965px) {
.section .pagetitle h1 {
font-size: 22px;
}

.section .pagetitle h2 {
font-size: 22px;
}

.pagetitle {
padding: 6px 0;
}

.section.titlesection .pagetitle {
padding: 0;
}

.h1, .section.titlesection .pagetitle h1 {
font-size: 24px;
}
}

/* END Títulos */

/* Texto legal */
.legales {
font-size: 12px;
background: rgba(0, 0, 0, 0.05);
padding: 20px 24px;
}

.legales.sub {
padding: 14px;
}

.legales p:last-child {
margin: 0;
}

/* END Texto legal */

/* Confirmación de form */

.mensaje {
position: fixed;
display: block;
margin: 0 auto;
top: 40px;
max-height: 100%;
width: 70%;
max-width: 100%;
left: 15%;
z-index: 1000000;
text-align: center;
padding: 1em;
font-size: 16px;
font-weight: bold;
border-radius: 10px;
}

.mensaje p {
padding: 0;
margin: 0;
}

.correcto {
background: rgb(176, 228, 176);
border: 1px solid rgb(1, 116, 1);
}

.incorrecto {
background: rgb(244, 157, 157);
border: 1px solid rgb(171, 6, 6);
}


.advertencia {
background: rgb(241, 187, 34);
border: 1px solid rgb(199, 136, 19);
}

input.incompleto, .inputwrapper .incompleto, .triviawrapper textarea.incompleto, .triviawrapper qna.incompleto textarea {
border: 1px solid rgb(171, 6, 6);
}
.qna.required.incompleto {
color: #ca0b0b;
}

/* END Confirmación de form */

/* Chat */
#designstudio-button {
display: none !important;
width: 0 !important;
height: 0 !important;
position: absolute !important;
z-index: -1 !important;
}
.-snapengage-tab {
display: none !important; /* oculta la solapa de chat en versión mobile */
}

/* Botón para chatear en tabla contáctenos */
.row.chatrow {
display: none;
}
/* END Botón para chatear en tabla contáctenos */

/* Botón online para chatear en tabla contáctenos */
.row.chatrow.online {
display: table-row;
}

@media (max-width: 699px) {
.row.chatrow.online {
/*display: block;*/
}
}

.row.chatrow.online .chatbutton {
/*background-color: #F99213; */
/*background-color: #FF9613;*/
background-color: #03b303;
}

.row.chatrow.online .chatbutton:hover {
background-color: #FFB227;
background-color: #068d06;
}

.row.chatrow.online .chatbutton:active {
background-color: #ED890C;
background-color: #046004;
}
/* END Botón online para chatear en tabla contáctenos */

/* Botón offline para chatear en tabla contáctenos */
/*
.row.chatrow.offline {
display: table-row;
}
*/

.row.chatrow.offline .chatbutton {
background-color: #5E749B;
}

.row.chatrow.offline .chatbutton:hover {
background-color: #6D91CA;
}

.row.chatrow.offline .chatbutton:active {
background-color: #4D6288;
}
/* END Botón offline para chatear en tabla contáctenos */

@media (max-width: 699px), (orientation: portrait) {
.SnapABug_Button, #snapengage-button {
display: none !important;
}
}

/* Solapa propia */
#solapachat {
font-weight: bold;
display: block;
margin: 0;
padding: 0;
border: 1px solid rgb(33, 139, 42);
border-right: 0;
border-radius: 5px 0 0 5px;
width: 30px;
height: 30px;
text-transform: uppercase;
cursor: pointer;
text-align: center;
background: url('/img/icono_mail.svg') rgb(57, 166, 67) no-repeat center;
background-size: cover;
position: fixed;
z-index: 999998;
right: 0;
top: 170px;
-webkit-transform-origin: 100% 50% 0px;
transform-origin: 100% 50% 0px;
}

#solapachat:hover {
box-shadow: 1px 1px 5px rgba(0, 0, 0, 0.54);
}

#solapachat:active {
box-shadow: 1px 1px 5px rgba(0, 0, 0, 0.42) inset;
border-color: rgb(25, 108, 32);
}

@media (max-width: 699px), (orientation: portrait) {
/* mobile */
#solapachat {
top: 75px;
}
}
/* END Solapa propia */

/* Ocultar botón */
body div#SnapABug_Button.SnapABug_Button {
display: none !important;
}
/* END Ocultar botón */

p.chatoffmsg {
background: transparent;
display: block;
margin-top: 0;
padding: 0;
font-size: 23px;
font-weight: normal;
}

/* END Chat */

/* estilos especiales */
.bold {
font-weight: bold;
}

.italic {
font-style: italic;
}

.size1 {
font-size: 1.3em;
line-height: 1.8;
}

@media (max-width: 350px) {
.size1 {
font-size: 1.2em;
}
}
/* END estilos especiales */

.noselect {
-webkit-touch-callout: none; /* iOS Safari */
-webkit-user-select: none; /* Chrome/Safari/Opera */
-khtml-user-select: none; /* Konqueror */
-moz-user-select: none; /* Firefox */
-ms-user-select: none; /* Internet Explorer/Edge */
user-select: none;
}

/* END global */

/* menu */
/* Estilos para el menú, mob & desk */

@media (min-width: 700px) {
	#menuwrapper ul li#iniciomob {
		display: none;
	}

	#menuwrapper {
		margin-right: 0 0 0 1em;
		padding: 0;
		background: transparent;
		display: inline-block;
		color: #727f88;
		text-transform: uppercase;
		float: right;
	}

	#menuwrapper a {
		width: 100%;
		color: inherit;
	}

	#menuwrapper a:hover,
	#menuwrapper a:visited,
	#menuwrapper a:link {
		color: inherit;
		text-decoration: none;
	}

	#menuwrapper > ul {
		display: inline-block;
		padding: 0;
		text-align: left;
		margin: 0 auto;
		width: 100%;
	}

	#menuwrapper ul li {
		display: inline-block;
		line-height: 1;
		margin: 0;
		padding: 0;
	}

	#menuwrapper > ul > li > a {
		font-weight: bold;
		font-size: 17px;
		margin: 0;
		padding: 0.3em 2em;
	}

	#menuwrapper ul li a {
		display: inline-block;
		line-height: 3;
	}

	#menuwrapper ul li ul {
		display: none;
		background: #efefef;
		width: 100%;
	}

	#menuwrapper ul > li > ul {
		/* clase añadida mediante js */
		display: none;
		position: absolute;
		padding: 1em 0;
		font-weight: bold;
	}

	#menuwrapper ul > li > ul > li > ul {
		/*background: #dedede;*/
		background: rgb(226, 226, 226);
		top: 0;
		margin-left: 100%;
		z-index: -1;
	}

	#menuwrapper ul > li > ul > li > ul > li {
		font-weight: normal;
		color: #1e1e1e;
	}

	#menuwrapper ul > li > ul > li > ul > li:hover {
		color: #000;
		/*background: #A8A8A8;*/
		/*background: #D1D1D1;*/
		background: rgb(215, 215, 215);
	}

	#menuwrapper ul li ul li {
		display: block;
		width: 100%;
		padding: 0;
		color: #848484;
	}

	#menuwrapper ul li ul li a {
		padding: 0 1.5em;
	}
}

@media (max-width: 699px), (orientation: portrait) {
	#menuwrapper ul > li > ul {
		position: relative;
	}
	/* Originalmente, este menú se animaba individualmente, pero ahora lo voy a dejar fijo a un costado del body, y animar el body solo, para reducir recursos */
	#menuwrapper {
		width: 233px;
		height: 100%;
		margin: 0 auto;
		padding: 0 0 47px 0; /* compensa la altura de los links sociales */
		position: absolute;
		/*top: -92px;*/
		top: 0;
		left: -233px;
		background: #f8f8f8;
		display: block;
		color: #727f88;
		text-transform: uppercase;
		overflow-y: auto;
		box-shadow: 0 0 1px transparent;
	}

	#menuwrapper a {
		width: 100%;
		color: inherit;
	}

	#menuwrapper a,
	#menuwrapper a:hover,
	#menuwrapper a:visited,
	#menuwrapper a:link {
		color: #181818;
		text-decoration: none;
	}

	#menuwrapper > ul {
		display: block;
		padding: 0;
		text-align: left;
		margin: 0;
		width: 100%;
	}

	#menuwrapper ul li {
		display: block;
		line-height: 1;
		margin: 0;
		padding: 0;
		width: 100%;
	}
	#menuwrapper > ul > li:after {
		content: '';
		width: 100%; /* Si no soporta calc() */
		width: calc(100% - 10px);
		height: 1px;
		background: silver;
		position: relative;
		margin-left: 10px;
		display: block;
	}

	#menuwrapper > ul > li > ul > li:after {
		content: '';
		height: 1px;
		background: silver;
		position: relative;
		margin-left: 20px;
		display: block;
	}
	#menuwrapper > ul > li > ul > li:last-child:after {
		height: 0;
	}

	#menuwrapper > ul > li > ul > li > ul > li:after {
		content: '';
		height: 1px;
		background: silver;
		position: relative;
		margin-left: 30px;
		display: block;
	}

	#menuwrapper ul li:hover,
	#menuwrapper ul li.hover {
		background: #efefef;
		color: #537a97;
	}

	#menuwrapper > ul > li > a {
		font-weight: bold;
		font-size: 15px;
		margin: 0;
		padding: 0 2em;
	}

	#menuwrapper ul li a {
		display: inline-block;
		line-height: 3;
		width: 100%;
		margin: 0;
		padding: 0 10px;
	}

	#menuwrapper ul li ul {
		display: none;
		background: #efefef;
		width: 100%;
	}

	#menuwrapper ul li ul li ul {
		display: none;
	}

	#menuwrapper ul > li > ul > li > ul {
		background: rgb(226, 226, 226);
		top: 0;
		margin-left: 0;
	}

	#menuwrapper ul > li > ul > li > ul > li {
		font-weight: normal;
		color: #1e1e1e;
	}

	#menuwrapper ul > li > ul > li > ul > li:hover {
		color: #000;
	}

	#menuwrapper ul li ul li {
		display: block;
		width: 100%;
		padding: 0;
		color: #848484;
	}

	#menuwrapper ul li ul li a {
		padding: 0 20px;
	}

	#menuwrapper ul li ul li ul li a {
		padding: 0 30px;
	}

	#menuwrapper ul li ul li:hover,
	#menuwrapper ul li ul li.subhover {
		background: #dedede;
		color: #626262;
	}

	@media (max-width: 699px), (orientation: portrait) {
		/* Menu adjust for desktops with small resolution */
		li#iniciomob,
		#menuwrapper ul li#iniciomob {
			display: block;
		}
	}

	.parenttop > a:after {
		content: '';
		background: transparent;
		width: 10px;
		height: 10px;
		display: inline-block;
		margin-top: 16px;
		margin-right: 9px;
		border-right: 2px solid rgba(0, 0, 0, 0.47);
		border-bottom: 2px solid rgba(0, 0, 0, 0.47);
		float: right;
		-webkit-transform: rotate(45deg);
		-ms-transform: rotate(45deg);
		transform: rotate(45deg);
	}

	.parenttop.hover > a:after,
	.parenttop.submenutoclose > a:after {
		-webkit-transform: rotate(225deg);
		-ms-transform: rotate(225deg);
		transform: rotate(225deg);
		margin-top: 20px;
	}

	.parentsub > a:after {
		content: '';
		background: transparent;
		width: 8px;
		height: 8px;
		display: inline-block;
		margin-top: 15px;
		border-right: 2px solid rgba(0, 0, 0, 0.47);
		border-bottom: 2px solid rgba(0, 0, 0, 0.47);
		float: right;
		-webkit-transform: rotate(45deg);
		-ms-transform: rotate(45deg);
		transform: rotate(45deg);
	}

	.parentsub.subhover > a:after {
		-webkit-transform: rotate(225deg);
		-ms-transform: rotate(225deg);
		transform: rotate(225deg);
		margin-top: 20px;
	}
}

.sticky-wrapper {
	display: block;
	height: 60px !important;
}

.headertop {
	background: #005f97;
	width: 100%;
	height: 60px;
}

.stickedmenu .headertop {
	background: #005f97;
}

.nav {
	max-width: 1080px;
	padding: 0 60px;
	display: block;
	margin: 0 auto;
	text-transform: uppercase;
}

.navmenu {
	list-style: none;
	padding-bottom: 10em;
	max-height: 30em;
	overflow: auto;
	margin: 0 auto -10em auto;
	height: 210px;
	overflow-x: scroll;
	white-space: nowrap;
	display: block;
	position: relative;
}

.nav ul {
	list-style: none;
	padding: 0;
	margin: 0;
}

.nav ul li {
	display: inline-block;
	height: 100%;
	color: white;
	padding: 0 12px;
	cursor: pointer;
	line-height: 45px;
}

.stickedmenu .nav ul li {
	color: white;
}

.nav ul li:hover,
.nav ul li.hovermenu {
	color: #bdbdbd;
}

#headermenucero {
	display: block;
	position: relative;
	z-index: 10;
	top: 0;
}

#headdoslogoinline {
	background: url('../img/logo_contenedor-19092018.svg') no-repeat
		center center;
	width: 152px;
	height: 60px;
	float: left;
	background-size: contain;
	margin: 0;
	position: absolute;
	top: -70px;
}

.stickedmenu #headdoslogoinline {
	position: relative;
	top: 0;
}

.stickedmenu #headdoslogoinline {
	background: url('../img/logo_texto-19092018.svg') no-repeat
		center center;
	background-size: contain;
}

#headdoslogointop {
	background: url('../img/logo_texto-19092018.svg') no-repeat
		center center;
	width: 120px;
	height: 46px;
	background-size: contain;
	display: block;
	margin: 0 auto;
}

#topmobile {
	display: none;
	position: fixed;
	z-index: 9;
	top: 0;
}

/* Button */
.menu-openerdos-inner-bottom,
.menu-openerdos-inner-top {
	-moz-transition: all 250ms 0.3s;
	-webkit-transition: all 250ms 0.3s;
	-o-transition: all 250ms 0.3s;
	transition: all 250ms 0.3s;
}

.menu-openerdos {
	/* I use this width as a mobile/desktop breakpoint in the js */
	display: block;
	width: 100px;
	max-width: 25%; /* Amplío el espacio clickeable cerca del botón */
	height: 46px;
	float: left;
	top: 0;
	left: 0;
	cursor: pointer;
	z-index: 1000;
}

.menu-openerdos-inner-top {
	background: #fff;
	margin-left: 12px;
	margin-top: 15px;
	width: 22px;
	height: 5px;
}

.menu-openerdos-inner-bottom {
	background: #fff;
	display: block;
	height: 5px;
	width: 22px;
	margin-left: 12px;
	margin-top: 6px;
}

.menuin .menu-openerdos-inner-top {
	-webkit-transform: translateY(5px) translateX(-2px) rotate(45deg);
	-ms-transform: translateY(5px) translateX(-2px) rotate(45deg);
	transform: translateY(5px) translateX(-2px) rotate(45deg);
}
.menuin .menu-openerdos-inner-bottom {
	-webkit-transform: translateY(-6px) translateX(-2px) rotate(135deg);
	-ms-transform: translateY(-6px) translateX(-2px) rotate(135deg);
	transform: translateY(-6px) translateX(-2px) rotate(135deg);
}

/* END Button */

#menuwrapper > ul > li {
	text-align: center;
}

#menuwrapper > ul > li > a {
	font-size: 16px;
	/*padding: 0 35px;*/
	padding: 0 23px;
	min-width: 100px;
	line-height: 60px;
	position: relative;
	z-index: 1000;
}

#menuwrapper > ul > li > ul {
	font-weight: bold;
}

#menuwrapper ul li ul li {
	text-align: left;
}

.menuin .menu-openerdos {
	width: 100%;
	max-width: 100%;
	height: 100vh;
}

.menuin {
	position: fixed;
	overflow: hidden;
	width: 100%;
	height: 100%;
	-webkit-transform: translateX(233px);
	-moz-transform: translateX(233px);
	-ms-transform: translateX(233px);
	transform: translateX(233px);
}

.menuin #topmobile {
	position: absolute;
}

@media (max-width: 699px), (orientation: portrait) {
	.sticky-wrapper {
		height: 0px !important;
	}

	.nav ul li:first-child {
		padding-left: 0;
	}

	.subslidemenuapple {
		overflow: hidden;
		top: 92px;
	}

	#headdoslogoinline {
		display: none;
	}

	#topmobile {
		display: block;
	}

	#headermenucero {
		display: none;
		background: rgb(0, 71, 114);
		top: 46px;
	}

	.navmenu {
		left: -100%;
		opacity: 0;
	}
}

@media (min-width: 700px) and (orientation: landscape) {
	#menuwrapper {
		display: table;
		width: 100%;
	}

	.stickedmenu #menuwrapper {
		display: block;
		width: auto;
	}

	#menuwrapper > ul > li {
		display: table-cell;
		width: 470px;
	}

	.stickedmenu #menuwrapper > ul > li {
		display: inline-block;
		width: auto;
	}

	#menuwrapper ul > li > ul {
		display: block;
		height: 0;
		top: 0;
	}

	#menuwrapper ul li ul li a:link,
	#menuwrapper ul li ul li a:hover,
	#menuwrapper ul li ul li a:visited {
		color: black;
	}

	#menuwrapper ul li ul.submenu,
	#menuwrapper ul > li > ul {
		width: auto;
		min-width: 200px;
		background: white;
		border: 1px solid #afafad;
		border-top: 0;
		padding: 6px;
		-moz-transition: opacity 0.3s ease, top 0.3s ease;
		-webkit-transition: opacity 0.3s ease, top 0.3s ease;
		-o-transition: opacity 0.3s ease, top 0.3s ease;
		transition: opacity 0.3s ease, top 0.3s ease;
		opacity: 0;
		width: 0px;
		overflow: hidden;
		z-index: 999;
	}

	.stickedmenu #menuwrapper ul li ul.submenu,
	.stickedmenu #menuwrapper ul > li > ul {
		border-top: 0;
	}

	#menuwrapper > ul > li > a {
		background: #005f97;
	}

	.stickedmenu #menuwrapper > ul > li > a {
		background: #005f97;
	}

	#menuwrapper > ul > li:hover,
	#menuwrapper > ul > li.hover,
	#menuwrapper > ul > li:hover > a {
		background: #004d7b;
		color: white;
	}

	.stickedmenu #menuwrapper > ul > li.parenttop:hover > a {
		border-bottom: 0px solid;
	}

	#menuwrapper ul li ul li a {
		white-space: nowrap;
	}

	#menuwrapper ul li ul li a:hover,
	#menuwrapper ul li ul li a:focus,
	#menuwrapper ul li ul li a:active {
		background: #e6e6e6;
	}

	#menuwrapper ul > li:hover > ul {
		opacity: 1;
		height: auto;
		top: 60px;
		width: auto;
	}
}

@media (max-width: 699px), (orientation: portrait) {
	#headermenucero {
		display: block;
		height: 0px;
		/* arreglos para los sociallinks */
		display: block;
		position: absolute;
		width: 0;
		height: 100%;
		top: 0;
		/* END arreglos para los sociallinks */
	}

	.headertop {
		background: #005f97;
		height: 46px;
	}

	#menuwrapper > ul > li {
		text-align: left;
	}

	#menuwrapper > ul > li > a {
		font-size: 16px;
		line-height: 46px;
		padding: 0 0 0 20px;
		width: 213px;
	}
}

@media (min-width: 1081px) and (max-width: 1100px) and (orientation: landscape) {
	.nav ul li {
		font-size: 1.5vw;
	}

	#menuwrapper > ul > li > a {
		font-size: 1em;
		padding: 0 1.3em;
	}
}

@media (min-width: 995px) and (max-width: 1080px) and (orientation: landscape) {
	.nav ul li {
		font-size: 1.4vw;
	}

	#menuwrapper > ul > li > a {
		font-size: 1em;
		padding: 0 0em;
	}
}

@media (min-width: 950px) and (max-width: 994px) and (orientation: landscape) {
	.nav ul li {
		font-size: 1.3vw;
	}

	#menuwrapper > ul > li > a {
		font-size: 1em;
		padding: 0 0em;
		min-width: 94px;
	}
}

@media (min-width: 860px) and (max-width: 949px) and (orientation: landscape) {
	.nav ul li {
		font-size: 1.5vw;
	}

	#menuwrapper > ul > li > a {
		font-size: 0.95em;
		padding: 0 1.3em;
		min-width: 50px;
	}
}

@media (min-width: 801px) and (max-width: 859px) and (orientation: landscape) {
	.nav ul li {
		font-size: 1.3vw;
	}

	#menuwrapper > ul > li > a {
		font-size: 0.95em;
		padding: 0 1.5em;
		min-width: 50px;
	}
}

@media (min-width: 700px) and (max-width: 800px) and (orientation: landscape) {
	.nav ul li {
		font-size: 1.3vw;
	}

	#menuwrapper > ul > li > a {
		font-size: 1em;
		padding: 0 2em;
		min-width: 50px;
	}
}

@media (max-width: 800px) {
	.nav {
		padding: 0 12px;
	}
}

/* END menu */
/* footer links */
/* Estilos para el pie de página; linkslist y texto al pie */

#mainfooter {
	padding: 30px 0;
	background: #F0F0F0;
	font-size: 13px;
}

.footer {
	width: 69%; /* Si no soporta calc() */
	width: calc(100% - 200px);
	display: inline-block;
}

.footer a {
	display: inline-block;
}

.footer ul, .footer li {
	list-style: none;
	padding: 0;
	margin: 0;
}

.footer li.title {
	padding: .5em;
	display: table-cell;
	width: 20%;
	cursor: default;
}

.footer li.title > a:hover {
	cursor: default;
	text-decoration: none;
}

li.title:nth-child(1), li.title:nth-child(2), li.title:nth-child(3) {
	width: 15%;
}

.footer > ul > li > ul {
	display: table;
}

#mainfooter .footer > ul {
	display: table;
	width: 100%;
}

#mainfooter .footer > ul > li {
	display: table-cell;
}

.title > a, .alltitles .pile a {
	color: #717171;
	font-weight: bold;
	text-transform: uppercase;
	font-size: 1em;
}

.title {
	display: inline-table;
}

.pile {
	display: inline-block;
	display: block;
}

.title .pile {
	display: block;
	border: 0;
}

.pile a {
	color: #717171;
	font-size: 1em;
	font-weight: normal;
}

.footernavigation {
	display: table-footer-group;
}


@media (max-width: 1080px) {
	#mainfooter {
		font-size: 1.1vw;
	}
}

@media (max-width: 800px) {
	#mainfooter {
		display: none;
	}
}

/* END footer links */

/* Footer legal */
.subfooter {
	font-size: .9em;
	line-height: 1;
	text-align: center;
	color: #7F7F7F;
	padding: 2rem 0;
	min-height: 90px;
}

#socialbottom {
	background: rgb(233, 233, 233);
	display: block;
	padding: 0;
	margin: 0;
	text-align: center;
}

#socialbottom ul {
	list-style: none;
	margin: 0;
	line-height: 0;
	padding: 0;
}

#socialbottom ul li {
	display: inline-block;
	padding: 20px 0 20px 25px;
}

#socialbottom ul li:first-child {
	padding-left: 0;
}

#socialbottom img {
	background: rgb(186, 186, 185);
	border-radius: 50%;
}

/* END Footer legal *//* Bricks */
/* Presenta pequños bloques de información, de forma responsive */

.xxxbrickscontainer {
	width: 100%; /* si no soporta calc() */
	width: calc(100% + 24px);
	max-width: 1080px;
	margin: 0 0 20px -12px;
	padding: 0;
	display: flex;
	text-align: left;
}

.xxxbrickwrap {
	width: 25%;
	padding: 12px 12px 24px 12px;
	display: inline-block;
}

@media (max-width: 910px) and (min-width: 661px) {
	.xxxbrickwrap {
		width: 33.33333333333333%;
	}
}

@media (max-width: 660px) and (min-width: 351px) {
	.xxxbrickwrap {
		width: 50%;
	}
}

@media (max-width: 350px) {
	.xxxbrickwrap {
		width: 100%;
	}
}

.xxxbrick {
	width: 100%;
	margin: 0;
	padding: 5px;
	display: inline-block;
	text-align: center;
	background: white;
	border: 1px solid silver;
	border-radius: 10px;
	height: 100%;
	padding-bottom: 26px;
	position: relative;
}

.xxxbrick header {
	width: 100%;
	height: 60px;
	display: block;
	margin: 5px auto;
}

.xxxbrick header img {
	position: relative;
	height: 100%;
	display: block;
	margin: 0 auto;
}

.xxxopenchannellist {
}

.xxxbrick span {
	display: block;
	margin: 5px auto;
}

.xxxbrick .xxxopenchannellist {
	color: black;
	border-radius: 5px;
	font-weight: bold;
	font-size: .9em;
	max-width: 100%;
	display: inline-block;
	padding: 0 .8em;
	margin-bottom: -1em;
	cursor: pointer;
	background: #fff;
    border: 1px solid rgb(182, 182, 182);
}

.xxxbrick .xxxopenchannellist:hover {
	background: rgb(139, 139, 139);
	cursor: pointer;
}

.xxxbrick .xxxchannel {
	font-weight: bold;
	text-transform: uppercase;
}

.xxxbrickprice {
	font-size: 3em;
	color: #662D91;
	font-weight: bold;
	line-height: .8;
	height: .8em;
	overflow: hidden;
}

@media (max-width: 525px) {
	.xxxbrickprice {
		font-size: 1.5em;
		font-weight: bold;
	}
}

.xxxbricklegal {
	font-size: 11px;
	width: 100%;
}

.xxxbrick .xxxbuttonget {
	position: absolute;
    bottom: -20px;
    left: 50%;
    transform: translate3d(-50%, 0, 0);
}






/* END Bricks *//* Slider */
.slick-slider
{
    position: relative;

    display: block;

    -moz-box-sizing: border-box;
         box-sizing: border-box;

    -webkit-user-select: none;
       -moz-user-select: none;
        -ms-user-select: none;
            user-select: none;

    -webkit-touch-callout: none;
    -khtml-user-select: none;
    -ms-touch-action: pan-y;
        touch-action: pan-y;
    -webkit-tap-highlight-color: transparent;
}

.slick-list
{
    position: relative;

    display: block;
    overflow: hidden;

    margin: 0;
    padding: 0;
}
.slick-list:focus
{
    outline: none;
}
.slick-list.dragging
{
    cursor: pointer;
    cursor: hand;
}

.slick-slider .slick-track,
.slick-slider .slick-list
{
    -webkit-transform: translate3d(0, 0, 0);
       -moz-transform: translate3d(0, 0, 0);
        -ms-transform: translate3d(0, 0, 0);
         -o-transform: translate3d(0, 0, 0);
            transform: translate3d(0, 0, 0);
}

.slick-track
{
    position: relative;
    top: 0;
    left: 0;

    display: block;
}
.slick-track:before,
.slick-track:after
{
    display: table;

    content: '';
}
.slick-track:after
{
    clear: both;
}
.slick-loading .slick-track
{
    visibility: hidden;
}

.slick-slide
{
    display: none;
    float: left;

    height: 100%;
    min-height: 1px;
}
[dir='rtl'] .slick-slide
{
    float: right;
}
.slick-slide img
{
    display: block;
}
.slick-slide.slick-loading img
{
    display: none;
}
.slick-slide.dragging img
{
    pointer-events: none;
}
.slick-initialized .slick-slide
{
    display: block;
}
.slick-loading .slick-slide
{
    visibility: hidden;
}
.slick-vertical .slick-slide
{
    display: block;

    height: auto;

    border: 1px solid transparent;
}/* slides */
/* Estilos para los slides, tanto grande como carousel */

	.sliderblock {
		display: block;
		width: 100%;
		margin: 0 auto 30px auto;
		/*overflow: hidden;*/
	}

	.sliderblockinner {
		background: transparent;
		width: 100%;
		width: calc(100% + 2.24vw); /* El doble del padding de .element a */
		margin-left: calc(-1.12vw); /* El padding de .element a */
	}

	.element {
	}

	.SlideAccesos .element {
		padding: 0 12px;
	}

	@media (max-width: 578px) {
		.SlideAccesos .element {
			padding: 0 7px;
		}
	}

	.SlideAccesos .element a {
		border: 2px solid silver;
		border-radius: 10px;
		overflow: hidden;
		background: white;
	}

	.element a {
		width: 100%;
		display: block;
		padding: 0 1.12vw;
		position: relative;
	}

	.SlideCarousel .sliderblockinner, .SlideAccesos .sliderblockinner {
		width: calc(100% + 24px); /* El doble del padding de .element a */
		margin-left: calc(-12px); /* El padding de .element a */
	}

	.sliderblockinner {
		visibility: hidden;
	}

	.sliderblockinner.started {
		visibility: visible;
	}

	.SlideCarousel .element a, .SlideAccesos .element a {
		padding: 0 12px;
	}

	@media (max-width: 578px) {
		.SlideCarousel .element a, .SlideAccesos .element a {
			padding: 0 7px;
		}

		.SlideCarousel .sliderblockinner, .SlideAccesos .sliderblockinner {
			width: calc(100% + 14px);
			margin-left: calc(-7px);
		}
	}

	.element a span {
		width: 100%;
		height: 1.4em;
		background: transparent;
		display: block;
		position: absolute;
		left: 0;
		text-align: center;
		z-index: 1;
		color: rgb(144, 141, 147);
		bottom: .1em;
		text-transform: uppercase;
		font-weight: bold;
		font-size: calc(12px + .1vw);
	}

	.element a img {
		width: 100%;
		height: 100%;
	}

	.SlideAccesos .element a img {
		padding: .8em .9em 1.8em .9em;
	}

	/* estado inicial de los elementos */
	/* Aún no está definido. Esto va acompañado por eventos en el slide.js
	.slick-slide {
		min-height: 100px;
		min-height: calc(25vw - 20px - 30px);
	}

	.element {
		visibility: hidden;
		position: relative;
	}

	.SlideAccesos .element a {
		position: absolute;
		width: 93%;
		height: 100%;
	}

	@media (max-width: 800px) {
		.slick-slide {
			min-height: 100px;
			min-height: calc(25vw - 20px);
		}
	}
	*/
	/* END estado inicial de los elementos */

	/* boques de info */
	.elementinfo {
		background: rgba(0, 0, 0, 0.8);
		color: white;
		text-transform: uppercase;
		font-size: 11px;
		display: block;
		position: absolute;
		bottom: 0;
		left: 8px;
		margin: 0;
		width: 100%;
		width: calc(100% - 16px);
		line-height: 1.3;
		padding: 5px 8px;
		text-align: center;
		height: auto;
		overflow: hidden;
		transition: bottom .2s;
	}

	/* modos */
		/* ModoUno */
		.modouno .elementinfo {
			bottom: -2.8em;
		}

		.modouno .element a:hover .elementinfo {
			bottom: 0;
		}
		/* END ModoUno */

		/* ModoDos */
		.mododos .elementinfo {
			bottom: -100%;
		}

		.mododos .element a:hover .elementinfo {
			bottom: 0;
			transition: bottom .4s;
		}
		/* END ModoDos */
	/* END modos */

	/* Etiquetas de tiempo */
	span.wheretag {
		display: inline-block;
		position: absolute;
		top: 0;
		right: 8px;
		text-align: center;
		padding: 3px 9px;
		line-height: 1;
		color: white;
		text-transform: capitalize;
	}

	span.wheretag.before {
		background: #C21B21;
	}

	span.wheretag.now {
		background: #00A01F;
		text-transform: uppercase;
	}

	span.wheretag.later {
		background: #0077CC;
	}
	/* END Etiquetas de tiempo */

	.elementinfo span {
		display: block;
		width: 100%;
		height: 1.3em;
		padding: 0;
		margin: 0;
		overflow: hidden;
	}

	.elementinfo span.title, .elementinfo span.extra {
		font-weight: bold;
	}
	/* END boques de info */

	/* Puntos */
	ul.slick-dots {
		/* display: inline-block; */
		list-style: none;
		width: 100%;
		text-align: center;
		padding: 0;
		margin: 0 auto;
		position: absolute;
		bottom: -30px
	}

	ul.slick-dots li {
		display: inline-block;
		width: 14px;
		height: 14px;
		margin: 0 6px;
	}

	@media (max-width: 359px) {
		ul.slick-dots li {
			width: 10px;
			height: 10px;
		}
	}

	ul.slick-dots li button {
		background: #EBEBEB;
		color: transparent;
		font-size: 0;
		line-height: 0;
		display: block;
		border: none;
		outline: none;
		width: 100%;
		height: 100%;
		border-radius: 50%;
	}

	ul.slick-dots li.slick-active button {
		background: #008FE2;
	}
	/* END Puntos */

	/* flechas */
	.slick-prev, .slick-next {
		background: red;
		color: transparent;
		font-size: 0;
		line-height: 0;
		border: none;
		outline: none;
		width: 39px;
		height: 70px;
		position: absolute;
		top: 36%;
		top: calc(50% - 20px - 15px); /* 1/2 altura del slide - 1/2 altura de flecha - 1/2 altura de dots */
	}

	.nodots .slick-prev, .nodots .slick-next {
		top: 36%;
		top: calc(50% - 20px - 7px);
	}

	.slick-next, .slick-next:active {
		background: url('/img/flecha_slider_der.png') no-repeat transparent;
		background-size: contain;
		right: -45px;
	}

	.slick-prev, .slick-prev:active {
		background: url('/img/flecha_slider_izq.png') no-repeat transparent;
		background-size: contain;
		left: -45px;
	}

	.slick-next:hover {
		background: url('/img/flecha_slider_der_over.png') no-repeat transparent;
		background-size: contain;
	}

	.slick-prev:hover {
		background: url('/img/flecha_sliver_izq_over.png') no-repeat transparent;
		background-size: contain;
	}

	.slick-next:active {
		background: url('/img/flecha_slider_der.png') no-repeat transparent;
		background-size: contain;
	}

	.slick-prev:active {
		background: url('/img/flecha_slider_izq.png') no-repeat transparent;
		background-size: contain;
	}

	@media (max-width: 800px) {
		.slick-next, .slick-prev {
			display: none !important;
		}

		.modouno .elementinfo, .mododos .elementinfo {
			bottom: 0;
		}
	}
	/* END flechas */

/* Buscador en slide en la web */
.element a span.buscadorpic {
	vertical-align: top;
	position: absolute;
	top: 0;
	height: 100%;
	width: 100%;
	display: block;
	background-repeat: no-repeat;
	background-position: center 0;
	display: block;
	padding: inherit;
	background-origin: content-box;
	background-size: contain;
}

.element a .slidewebeventinfo {
	height: 33%;
	min-height: 67px;
	padding: 8px 0;
	background: rgb(101, 101, 101);
	position: absolute;
	bottom: 0;
	width: calc(100% - 24px);
	z-index: 1;
}

.element a .slidewebeventinfo span {
	display: block;
	position: relative;
	width: 100%;
	color: whitesmoke;
	font-size: 12px;
	text-overflow: ellipsis;
	overflow: hidden;
	white-space: nowrap;
	padding: 0 5px;
}

.element a .slidewebeventinfo span.channel.channelnum {
	padding: 6px 0 0 0;
	height: 22px;
}

.element a .slidewebeventinfo span span.channelnum {
	background: whitesmoke;
	display: inline-block;
	width: auto;
	color: #656565;
	height: 17px;
}

@media (min-width: 560px) and (max-width: 1040px) {
	.element a .slidewebeventinfo {
		padding: 6px 0;
	}
}

@media (min-width: 560px) and (max-width: 655px), (max-width: 430px) {
	.element a .slidewebeventinfo {
		min-height: 50px;
	}

	.element a .slidewebeventinfo span.channel.time {
		display: none;
	}
}

@media (max-width: 578px) {
	.element a .slidewebeventinfo {
		width: calc(100% - 14px);
	}
}

@media (min-width: 530px) and (max-width: 546px) {
	.element a .slidewebeventinfo {
		padding: 12px 0;
	}
}

@media (min-width: 431px) and (max-width: 477px), (max-width: 367px) {
	.element a .slidewebeventinfo {
		padding: 6px 0;
	}
}
/* END Buscador en slide en la web */
/* Ficha del buscador en la web */

.ficha .social {
	width: 100%;
	height: 30px;
	display: block;
	vertical-align: bottom;
	text-align: center;
	margin: 10px 0;
	padding: 20px 0 0 0;
	/*border-top: 1px solid silver;*/
	box-sizing: content-box;
}

.ratingwrapper {
	display: inline-block;
	margin-right: 20px;
	margin-top: 5px;
	margin-bottom: 5px;
	height: 13px;
	line-height: 13px;
	vertical-align: top;
	position: relative;
}

.ficha .social ul {
	list-style: none;
	display: block;
	margin: 0;
	padding: 0;
}

.ficha .social ul li {
	display: inline-block;
	margin: 0;
	padding: 0;
	vertical-align: middle;
	height: 30px;
}

.ficha .social ul li + li {
	margin-left: 20px;
}

.ficha .social ul li a {
	display: inline-block;
	height: 30px;
	width: 30px;
	background: rgb(186, 186, 185);
	padding: 0;
	margin: 0;
	border-radius: 50%;
	overflow: hidden;
}

.ficha .social ul li img {
	width: 100%;
}

.ficha .social ul li a.Facebooklink img {
	background-color: #3b5998;
}

.ficha .social ul li a.Twitterlink img {
	background-color: #55ACEE;
}

.ficha .social ul li a.WhatsApplink img {
	background-color: #25D366;
}

.ficha .social ul li a.Permalink img {
	background-color: #FF930B;
}

/* Ficha */
	.ficha {
		display: block;
		width: 500px;
		max-width: 100%;
		font-size: 16px;
	}

	.ficha .eventdetails {
		display: block;
	}

	.eventdetailgroup {
		display: table-row;
	}

	.ficha .pic {
		display: table-cell;
		margin: 0 0 0 0;
		padding: 0 0 0 0;
		width: 222px;
		max-width: 100%;
		height: 150px;
	}

	.tecdetails {
		display: table-cell;
		vertical-align: top;
	}

	.pic + .tecdetails {
		padding-left: 10px;
	}

	.ficha h3.title {
		display: block;
		margin: 0 0 4px 0;
		padding: 0;
	}

	p.info {
		margin: 0;
		padding: 0;
	}

	p.info + p.info {
		margin-top: 12px;
	}

	.eventdetailgroup + p.info:first-of-type {
		margin-top: 12px;
	}

	.info > span {
		display: inline;
		line-height: 1;
	}

	.info.basics span span {
		white-space: nowrap;
	}

	.info > span + span, .info.ocurrencias > span span + span {
		border-left: 1px solid silver;
		padding-left: 5px;
		margin-left: 5px;
	}

	.info > span.subtitle + span {
		padding: 0px;
		margin: 0px;
		border: none;
	}

	.info.ocurrencias > span span, .info.ocurrencias > span span + span {
		margin-left: 0;
		margin-right: 5px;
	}

	.info.ocurrencias {
		margin: 10px 0;
	}

	.info.ocurrencias > span {
		border: 0;
		display: block;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		line-height: 1.4;
		font-weight: bold;
		text-transform: uppercase;
	}

	.info.ocurrencias > span span {
		display: inline;
		display: inline-block;
		font-weight: normal;
		text-transform: none;
	}

	.info.ocurrencias > span span.ocurrenciacanal {
		font-weight: bold;
	}

	p.info.sinopsis, .info.ocurrencias, p.info + .info.ocurrencias {
		display: block;
		margin: 10px 0 0 0;
		padding: 10px 0 0 0;
		/*border-top: 1px solid silver;*/
		overflow: hidden;
	}

	.filtrable {
		cursor: pointer;
	}
	
	.filtrable:hover {
		/*text-decoration: underline;*/
	}

	p.info .filtrable, .subtitle + span {
		color: #005A96;
	}

	p.info.basics .filtrable {
		color: inherit;
	}

	.partidoequipos {
		text-align: center;
		padding: 10px 0 0 0;
		margin: 10px 0 0 0;
	}

	.topinfo {
		display: table;
		position: relative;
		min-height: 150px;
	}

	.topinfobottomwrapper {
		display: table-row;
		display: inline-block;
		width: 100%;
	}

	.topinfobottom {
		display: table-cell;
		position: relative;
		vertical-align: bottom;
	}

	/* Ocurrencias */
	.ocurrenciacanalbloque {
		display: block;
		width: calc(100% + 30px);
	}

	.ocurrenciacanalbloque + .ocurrenciacanalbloque {
		margin-top: 12px;
	}

	.canaltitle {
		font-weight: bold;
		font-size: 18px;
		margin-bottom: 5px;
	}

	.ocurrenciadiabloque {
		display: inline-block;
		vertical-align: top;
		width: auto;
		max-width: 14.28%;
	}

	.emisionhorario {
		display: inline-block;
		width: 100%;
	}

	.daytitle {
		display: inline-block;
		width: 100%;
		font-weight: bold;
		font-size: 18px;
	}

	@media (max-width: 550px) {
		.ocurrenciacanalbloque {
			width: 100%;
		}
	}

	@media (max-width: 380px) {
		.ocurrenciadiabloque {
			font-size: 13px;
		}

		.daytitle {
			font-size: 14px;
		}
	}
	/* END Ocurrencias */
	
	/* Rating */
	.rating.basis {
		display: inline-block;
		background-image: url('/img/estrellasmagentas-01-01.svg');
		background-position: 0 13px;
		background-size: 70px 26px;
		width: 70px;
		height: 13px;
		vertical-align: top;
	}

	.rating.basis + .rating {
		display: inline-block;
		background: url('/img/estrellasmagentas-01-01.svg');
		background-position: 0 0;
		background-size: 70px 26px;
		width: 100%;
		height: 100%;
		vertical-align: bottom;
		margin: 0;
		padding-right: 0;
		box-sizing: content-box;
		position: absolute;
		top: 0;
		left: 0;
	}

	.rating.basis + .rating.puntos1 {
		width: 7px;
	}

	.rating.basis + .rating.puntos2 {
		width: 14px;
	}

	.rating.basis + .rating.puntos3 {
		width: 21px;
	}

	.rating.basis + .rating.puntos4 {
		width: 28px;
	}

	.rating.basis + .rating.puntos5 {
		width: 35px;
	}

	.rating.basis + .rating.puntos6 {
		width: 42px;
	}

	.rating.basis + .rating.puntos7 {
		width: 49px;
	}

	.rating.basis + .rating.puntos8 {
		width: 56px;
	}

	.rating.basis + .rating.puntos9 {
		width: 63px;
	}

	.rating.basis + .rating.puntos10 {
		width: 70px;
	}
	/* END Rating */

	.info.originaltitle > span.h3 {
		display: block;
		margin: 0;
		padding: 0;
		padding-bottom: 0;
	}

	p.info span.subtitle {
		border: none;
		padding: 0;
		margin: 0;
		width: 100%;
		display: inline-block;
	}

	/* Mobile */
	@media (max-width: 450px) {
		.ficha .pic {
			display: block;
			float: right;
			/*width: 35%;*/
			width: 50%;
			height: auto;

			float: none;
			width: 100%;
		}

		.topinfo {
			min-height: 0;
		}
		
		/*
		.tecdetails {
			display: table-cell;
			vertical-align: top;
			display: block;
		}

		.eventdetailgroup {
			display: table;
			width: 100%;
		}

		.pic + .tecdetails {
			padding-left: 0;
		}
		*/
	}
	/* END Mobile */
/* END Ficha */
/* END Ficha del buscador en la web *//* canales */
/* Estilos para presentar los bricks de canales, incluyendo opciones wrapped */
.xxxchannelscontainer {
	height: 472px;
	height: 477px;
	max-height: 50vh;
	overflow: hidden;
	display: block;
	width: 100%;
	margin: 12px 0 0 0;
}

.xxxchannelscontainer.border {
	border-top: 1px solid rgba(192, 192, 192, 0.26);
}

.xxxchannelsbrickscontainer {
	height: 100%;
	overflow: hidden;
	display: block;
	width: 100%;
	width: calc(100% + 12px);
	padding: 0;
	margin: -12px;
	background: transparent;
}

.xxxchannelsbrickscontainer.border {
	border-bottom: 1px solid rgba(192, 192, 192, 0.26);
}

.xxxchannelbrickwrap {
	padding: 12px;
	display: inline-block;
	width: 16.66666666666666%;
}

@media (max-width: 950px) and (min-width: 761px) {
	.xxxchannelbrickwrap {
		width: 20%;
	}
}

@media (max-width: 760px) and (min-width: 571px) {
	.xxxchannelbrickwrap {
		width: 25%;
	}
}

@media (max-width: 570px) and (min-width: 351px) {
	.xxxchannelbrickwrap {
		width: 33.3333333333%;
	}
}

@media (max-width: 350px) and (min-width: 201px) {
	.xxxchannelbrickwrap {
		width: 50%;
	}
}

@media (max-width: 200px) {
	.xxxchannelbrickwrap {
		width: 100%;
	}
}

.xxxchannelsbrick {
	border: 1px solid silver;
	display: inline-block;
	padding: 1em 0 0 0;
	margin: 0;
	text-align: center;
	width: 100%;
	border-radius: .6em;
	background: white;
	cursor: pointer;
}

.xxxchannelsbrick header {
	background: no-repeat center center;
	background-size: contain;
	display: block;
	margin: 0 auto;
	max-width: 100%;
	width: 60%;
	max-width: 80px;
	padding-bottom: 60%;
}

.xxxchannelname {
	display: block;
	padding: 0;
	margin: 0 auto;
	text-transform: uppercase;
	text-align: center;
}

.xxxchannelnumber {
	display: block;
	padding: 0;
	margin: 0 auto;
	font-weight: bold;
	text-align: center;
}


/* Custom Scrollbar */

/* scroll viewport */
.scrollable {
	position: relative;
}

.scrollable:focus {
	outline: 0;
}

.scrollable .viewport {
	position: relative;
	overflow: hidden;
}

.scrollable .viewport .overview {
	position: relative;
}

.scrollable .scroll-bar {
	display: none;
}

.scrollable .scroll-bar.vertical {
	position: absolute;
	right: 0;
	height: 100%;
}

.scrollable .scroll-bar .thumb {
	position: absolute;
}

.not-selectable {
	-webkit-touch-callout: none;
	-webkit-user-select: none;
	-khtml-user-select: none;
	-moz-user-select: none;
	-ms-user-select: none;
	user-select: none;
}
/* END scroll viewport */

/* scrollbar style */

.scrollable.scrollbar .scroll-bar {
	border-left: 1px solid silver;
	background-color: transparent;
}

.scrollable.scrollbar .scroll-bar.vertical {
	width: 0;
	top: 12px;
	right: 4px;
	z-index: 1;
}

.scrollable.scrollbar .scroll-bar .thumb {
	background-color: rgb(107, 107, 107);
	width: 3px;
	margin-left: -2px;
}



.thumb:active, .thumb:focus, .scrollable.scrollbar .scroll-bar:active .thumb, .scrollable.scrollbar .scroll-bar:focus .thumb {
	width: 9px;
	margin-left: -5px;
	cursor: pointer;
	background-color: black;
}

.scrollable.scrollbar .scroll-bar:hover .thumb {
	width: 9px;
	margin-left: -5px;
	-moz-transition: width .1s, margin-left .1s;
	-webkit-transition: width .1s, margin-left .1s;
	-o-transition: width .1s, margin-left .1s;
	transition: width .1s, margin-left .1s;
	cursor: pointer;
}



.scrollable.scrollbar .scroll-bar:hover .thumb {
	background-color: black;
}
/* END scrollbar style */

/* END Custom Scrollbar */

/* Filter */

.filtercategory {
	width: 100%;
	text-align: right;
}

@media (max-width: 646px) {
	.filtercategory {
		text-align: left;
	}
}

.filterchannels.tres {
	display: block;
	margin: 0 auto;
	width: 380px;
	max-width: 100%;
}

.filterchannels.tres .filterpackbutton {
	display: inline-block;
	width: 33.3333333333333333%;
	padding: 10px 10px 5px 10px;
}

.filterchannels.tres a {
	cursor: pointer;
}

.filterchannels.tres a.deselect {
	cursor: pointer;
	max-width: 100%;
	display: block;
	margin: 0 auto;
	width: 96px;
	border-radius: 12px;
}

.filterchannels.tres a.deselect img {
	opacity: .5;
}

select.catselect, .catselectwrapper > select {
	padding: .5em 1em;
	margin: 1em auto;
	display: inline-block;
	cursor: pointer;
	-webkit-appearance: none;
	-moz-appearance: none;
	border: 1px solid silver;
	border-radius: 3px;
	background-image: url('../img/selectarrow.gif');
	background-size: 10px 10px;
	background-repeat: no-repeat;
	background-position: right center;
	background-position: calc(100% - 8px) center;
	padding-right: 20px;
	background-color: white;
}

.filterpremiumwrapper {
	padding: 5px 0 0 0;
	margin: 1em auto;
	display: inline-block;
	cursor: pointer;
	vertical-align: middle;
	width: 160px;
	height: 36px;
	vertical-align: top;
}

@media (max-width: 646px) {
	.filterpremiumwrapper {
		float: right;
	}
}

/* END Filter */

/* En modal */
.chmodalcontent {
	background: transparent;
	width: 940px;
	max-width: 90vw;
}
/* END En modal */

	/* Selector temporal */
	.selctslide {
		display: table;
		width: 100%;
	}

	.selctslide ul {
		list-style: none;
		padding: 0;
		margin: 0;
		display: table-row;
	}

	.selctslide ul li {
		list-style: none;
		margin: 0;
		padding: 0;
		display: table-cell;
		width: 33.333333333333333333333333%;
		text-align: center;
		vertical-align: middle;
		cursor: pointer;
	}

	.selctslide ul li h1 {
		margin: 0;
	}

	.selctslide ul li.unselected h1 {
		color: rgba(111, 111, 111, 0.46);
	}

	.selctslide ul li:hover h1 {
		color: #2491DA;
	}

	.selctslide ul li:active h1 {
		color: #054B79;
	}

	.selctslide ul li.unselected:hover h1 {
		color: rgba(169, 169, 169, 0.52);
	}

	.selctslide ul li.unselected:active h1 {
		color: rgba(169, 169, 169, 0.52);
	}

	.selctslide ul li.unselected:active h1 {
		color: rgba(88, 88, 88, 0.49);
	}

	.selctslide ul li:nth-of-type(1) {
		text-align: left;
	}

	.selctslide ul li:nth-of-type(3) {
		text-align: right;
	}

	.selctslide .pagetitle {
		padding-top: 6px;
		padding-bottom: 6px;
	}

	/* selector dentro de modal */
	.chmodalcontent .selctslide {
		background: rgb(242, 242, 242);
		margin: 0 0 10px 0;
		padding: 12px 0;
	}
	.chmodalcontent .selctslide ul li:nth-of-type(1) {
		text-align: center;
	}

	.chmodalcontent .selctslide ul li h1 {
		font-size: 30px;
		line-height: 30px;
	}

	.chmodalcontent .selctslide ul li:hover h1 {
		color: #005A96;
	}
	/* END selector dentro de modal */
	/* END Selector temporal */

	/* Checkboxes */
	.check input {
		display: none;
	}

	.check label {
		display: block;
		width: 27px;
		height: 27px;
		margin: 3px 0 0 0;
		padding: 0;
		cursor: pointer;
		border-radius: 4px;
		border: 3px solid #005A96;
	}


	.check input:checked + label {
		background-image: url('../img/tildenaranja.svg');
		background-repeat: no-repeat;
		background-size: 17px 17px;
		background-position: center center;
	}
	/* END Checkboxes */

	/* check items */
	.checkitems {
		display: block;
		width: 400px;
		max-width: 100%;
		padding: 0;
		margin: 15px 0;
		white-space: nowrap;
	}

	.filterpremiumwrapper .checkitems, .filterpremiumwrapper .checkitems.onright {
		text-align: right;
	}

	.checkitems:first-child {
		margin-top: 0;
	}

	.checkitems:last-child {
		margin-bottom: 0;
	}

	.checkitems span, .checkitems label {
		display: inline-block;
		padding: 0;
		margin: 0;
		vertical-align: bottom;
		line-height: 27px;
		color: rgb(0,86,148);
		font-weight: bold;
		cursor: pointer;
	}

	.checkitems .check {
		/*float: right;*/
		margin-left: 1em;
	}

	@media (max-width: 489px) {
		.checkitems {
			width: 100%;
		}

		.checkitems .check {
			float: right;
		}
	}

	.checkitems label {
	}
	/* END check items */

/* Buscador de canales en la guía */
.findchinfilter {
	display: inline-block;
	vertical-align: top;
	float: left;
	text-align: left;
	width: calc(33.33333333333333% - 20px);
}

@media (max-width: 950px) and (min-width: 761px) {
	.findchinfilter {
		width: calc(40% - 19px);
	}
}

@media (max-width: 760px) and (min-width: 647px) {
	.findchinfilter {
		width: calc(50% - 18px);
	}
}

@media (max-width: 646px) {
	.findchinfilter {
		width: 100%;
	}
}

.findchwrapper {
	display: inline-block;
	width: 100%;
	vertical-align: bottom;
	padding: 0;
	box-sizing: content-box;
	position: relative;
}

.findchwrapper input.inputtext {
	border-radius: 3px;
	height: 36px;
	min-height: 36px;
	line-height: 36px;
	padding: 0 35px 0 14px;
	border: 1px solid rgba(0, 0, 0, 0.12);
}

.findchinfilter .findchwrapper input.inputtext {
	border-color: silver;
	margin: 14px 0;
}

.findchwrapper input.inputtext.activesearching {
	border-color: #2196f3;
}

.findchwrapper input.inputtext.activesearching.noclose {
	border-color: #ff6347;
}

.findchwrapper input.inputtext.activesearching.suggesting {
	border-bottom-left-radius: 0;
	border-bottom-right-radius: 0;
}

.xxxchannelscontainer.searchingch .xxxchannelbrickwrap {
	display: none !important;
}

.xxxchannelscontainer.searchingch .xxxchannelbrickwrap.searched {
	display: inline-block !important;
}

.xxxchannelscontainer.searchingch .xxxchannelbrickwrap.searched.precisesearch .xxxchannelsbrick { /* el canal preciso que se busca (sin la familia) */
}

.clrinput {
	display: none;
	position: absolute;
	background: transparent;
	width: 32px;
	height: 32px;
	top: calc(50% - 16px);
	right: 11px;
	overflow: hidden;
	color: #0089e7;
	cursor: pointer;
}

.noclose + .clrinput {
	color: #f11515;
}

.findchwrapper input.inputtext.activesearching + .clrinput {
	display: block;
}

.clrinput:before, .clrinput:after {
	content: "";
	width: 12px;
	height: 2px;
	background: currentColor;
	display: block;
	position: absolute;
	left: calc(50% - 4px);
	top: calc(50% - 1px);
	transform-origin: center;
}

.clrinput:before {
	transform: rotate(45deg);
}

.clrinput:after {
	transform: rotate(-45deg);
}

.selctslide.pausedfilter .pagetitle, .filtercategory.pausedfilter .checkitems, .filtercategory.pausedfilter select.catselect {
	cursor: default;
}

.selctslide.pausedfilter .pagetitle h1, .selctslide.pausedfilter ul li.unselected:hover h1 {
	color: rgba(169, 169, 169, 0.52);
}

.filtercategory.pausedfilter select.catselect, .filtercategory.pausedfilter .checkitems label {
	color: rgb(136, 136, 136);
}

.filtercategory.pausedfilter .check input:checked + label {
	background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxOS4yLjEsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjxzdmcgdmVyc2lvbj0iMS4xIiBpZD0iQ2FwYV8xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiB4PSIwcHgiIHk9IjBweCINCgkgdmlld0JveD0iMCAwIDMwIDMwIiBzdHlsZT0iZW5hYmxlLWJhY2tncm91bmQ6bmV3IDAgMCAzMCAzMDsiIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4NCgkuc3Qwe2ZpbGw6bm9uZTtzdHJva2U6IzYzNjM2MztzdHJva2Utd2lkdGg6Ni4xODg7c3Ryb2tlLWxpbmVjYXA6cm91bmQ7c3Ryb2tlLWxpbmVqb2luOnJvdW5kO3N0cm9rZS1taXRlcmxpbWl0OjEwO30NCjwvc3R5bGU+DQo8cG9seWxpbmUgY2xhc3M9InN0MCIgcG9pbnRzPSIzLjUsMTUuOCAxMS45LDI0LjEgMjYuNSw2LjcgIi8+DQo8L3N2Zz4NCg==');
}

.filtercategory.pausedfilter select.catselect {
	background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxOS4yLjEsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjxzdmcgdmVyc2lvbj0iMS4xIiBpZD0iQ2FwYV8xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiB4PSIwcHgiIHk9IjBweCINCgkgdmlld0JveD0iMCAwIDEwIDEwIiBzdHlsZT0iZW5hYmxlLWJhY2tncm91bmQ6bmV3IDAgMCAxMCAxMDsiIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4NCgkuc3Qwe2ZpbGw6IzYzNjM2Mzt9DQo8L3N0eWxlPg0KPHBhdGggY2xhc3M9InN0MCIgZD0iTTkuNiwzTDUuNCw3LjdjLTAuMiwwLjItMC42LDAuMi0wLjgsMEwwLjQsMy4xYy0wLjQtMC40LTAuMS0xLDAuNC0xaDguM0M5LjcsMi4xLDkuOSwyLjcsOS42LDN6Ii8+DQo8L3N2Zz4NCg==');
}

.section.showoff {
	animation: .25s showoff linear;
	animation-iteration-count: 2;
}

/*
.searched .xxxchannelnumber {
	display: inline-block;
	padding: 0 5px 0 0;
}

.searched.clasicodigital .xxxchannelnumber:before, .searched.digital .xxxchannelnumber:before, .searched.premium .xxxchannelnumber:before {
	content: "D: ";
}

.searched.hd .xxxchannelnumber:before {
	content: "HD: ";
}

.searched.clasico .xxxchannelnumber:before {
	content: "B: ";
}
*/

.suggestions {
	background: white;
	display: none;
	position: absolute;
	top: 50px;
	border: 1px solid rgba(0, 0, 0, 0.12);
	border-top: 0;
	width: 100%;
	margin: 0;
	padding: 0;
	border-radius: 0 0 3px 3px;
	z-index: 1;
	/* opacity: .9; */
	/* color: black; */
	max-height: 90px;
	overflow-y: auto;
}

.findchwrapper input.inputtext.activesearching.suggesting ~ .suggestions {
	display: block;
}

.suggestions span {
	text-transform: uppercase;
	display: inline-block;
	width: 100%;
	margin: 0;
	padding: 5px 14px;
	cursor: pointer;
}

.suggestions span:hover, .suggestions span.hover {
	background-color: #e4e4e4;
}


.chmsg {
	display: block;
	width: calc(100% - 12px);
	margin-left: 12px;
	text-align: center;
	text-transform: uppercase;
	font-size: 24px;
}

a.chautofilter, a.chautofilter:hover, a.chautofilter:active {
	cursor: pointer;
	text-decoration: underline;
}

a.chautofilter.xxxbutton, a.chautofilter.xxxbutton:hover, a.chautofilter.xxxbutton:active {
	text-decoration: none;
	background-color: #ff2793;
	font-size: 21px;
}

@keyframes showoff {
	0% {
		background-color: #2196f3;
	}
	10% {
		background-color: #2196f3;
	}
	100% {
		background-color: transparent;
	}
}
/* END Buscador de canales en la guía */

/* END canales */
/* bannerdeproducto */
/* Estilos para maquetar el banner de producto */

/* Banner image */
.fullimagebanner {
	display: block;
	width: 100%;
	/*height: 420px;*/
	height: 100%;
	/* padding-top: 43.75%; */
	background-size: cover;
	background-position: center;
	background-repeat: no-repeat;
	/* background-color: silver; */
	margin: 0 0 20px 0;
}
.fullimagebanner.border_radius_banner {
	border-radius: 10px;
}
/* END Banner image */

/* Versión desktop */
.xxxprodbannerdesk {
	width: 100%;
	border-radius: 10px;
	height: 420px;
	position: relative;
	margin-bottom: 20px;
	z-index: 1;
}

.xxxprodbannerdeskimagen {
	height: 420px;
	width: 76%; /* si no soporta calc() */
	width: calc(100% - 230px);
	position: absolute;
	z-index: -2;
	border-radius: 10px 0 0 10px;
	background-position: top center;
	background-repeat: no-repeat;
	background-color: silver;
	background-size: auto 100%;
	overflow: hidden;
}

a.bglink {
	display: block;
	width: 100%;
	height: 100%;
	background-color: transparent;
}

.xxxcartelprodbannerdesk {
	float: right;
	background: white;
	border-radius: 10px;
	height: 420px;
	width: 240px;
	padding: 20px 10px;
	text-align: center;
	border: 1px solid silver;
}

.xxxcartelprodbannerdesk > header > img {
	width: 129px;
	height: 129px;
}

a.picbanner {
	width: 129px;
	height: 129px;
	display: block;
	margin: 0 auto;
}

a.picbanner > img {
	width: 100%;
	display: block;
	margin: 0 auto;
}

.tablecell.side a.picbanner {
	width: 100%;
	height: 100%;
}

.xxxprodbannerdesk .xxxbuttonget {
	width: 120px;
	/* margin-bottom: -47px; */
	position: absolute;
	bottom: -20px;
	right: 58px;
}

.xxxprodbannerdeskinfo {
	margin: 10px 0;
	line-height: 1.4;
	height: 4.1em;
	text-align: center;
	width: 100%;
}

.xxxprodbannerdeskinfo p {
	margin: 0;
	width: 100%;
}

.xxxprodbannerdeskinfo p > span {
	display: inline-block;
	white-space: nowrap;
}

.xxxprodbannerdesk .xxxopenchannellist {
	display: inline-block;
	border-radius: 5px;
	font-weight: bold;
	padding: 0 0.8em;
	margin-top: 10px;
	cursor: pointer;
	background: #fff;
	color: black;
	border: 1px solid rgb(182, 182, 182);
}

.xxxopenchannellist a,
.xxxopenchannellist a:hover {
	color: inherit;
	text-decoration: none;
}

.xxxprodbannerdesk .xxxopenchannellist:hover {
	background: rgb(182, 182, 182);
	cursor: pointer;
}

.xxxprodbannerdeskprice {
	display: block;
	font-weight: bold;
	font-size: 50px;
	/*color: orangered;*/
	color: #662d91;
	padding: 0;
	line-height: 1;
	margin: 30px 0 10px 0;
}

.xxxprodbannerdeskprice span {
	font-size: 0.6em;
	display: inline-block;
	vertical-align: middle;
	line-height: 1;
	padding: 0 0 0.25em 0;
	margin-left: -0.2em;
}

.xxxprodbannerdesk p {
	display: block;
	margin: 0;
	width: 100%;
	text-align: center;
}

@media (max-width: 742px) and (min-width: 350px) {
	.xxxprodbannerdesk {
		display: none;
	}
}

/* Versión intermedia */
.xxxprodbannerdeskimagen.halfimage {
	display: none;
}
@media (max-width: 970px) {
	.xxxprodbannerdeskimagen.halfimage {
		display: block;
		background-size: cover;
	}

	.xxxprodbannerdeskimagen.halfimage + .xxxprodbannerdeskimagen {
		display: none;
	}
}
/* END Versión intermedia */

@media (max-width: 349px) {
	.xxxprodbannerdeskimagen,
	.xxxprodbannerdeskimagen.halfimage {
		display: none;
	}

	.xxxcartelprodbannerdesk {
		width: 100%;
		height: auto;
	}
}

/* END Versión desktop */

/* Combos 2 productos */
.comboicons {
	display: block;
	width: 100%;
	height: 99px;
	margin-top: 30px;
}

.comboicons img {
	display: inline-block;
	max-width: 70px;
}

span.comboplus {
	display: inline-block;
	font-weight: bold;
	font-size: 18px;
	vertical-align: middle;
	padding: 5px;
	color: rgb(12, 81, 125);
}

/* versión bricks */
.comboiconsbricks {
	display: block;
	width: 100%;
	height: 100px;
}

.xxxinternetbrick header .comboiconsbricks img {
	height: 57px;
	display: inline-block !important;
	width: 51px;
	margin: 0 10px;
}
/* END versión bricks */

/* END Combos 2 productos */

/* Versión mobile */
.xxxprodbannermob {
	display: none;
}

@media (max-width: 742px) and (min-width: 350px) {
	.xxxprodbannermob {
		display: block;
		position: relative;
		width: 100%;
		padding-top: 55%;
		border-radius: 10px;
		margin-bottom: 20px;
		background-size: 100% auto;
		background-repeat: no-repeat;
		background-color: silver;
		overflow: hidden;
	}

	.xxxcartelprodbannermob {
		border: 1px solid silver;
		border-radius: 10px;
		background: white;
		margin: 0;
		padding: 5px;
		display: block;
		width: 100%;
		z-index: 1;
		position: relative;
	}

	a.bglinkmob {
		display: block;
		width: 100%;
		height: 100%;
		position: absolute;
		top: 0;
		z-index: 0;
	}

	.xxxcartelprodbannermob .tablestyle {
		width: 100%;
	}

	.xxxprodbannermob .tablecell {
		vertical-align: middle;
		text-align: center;
		padding: 5px;
	}

	.xxxprodbannermob .tablestyle .tablecell.side {
		/*max-width: 93px;*/
		/*min-width: 60px;*/
		width: 70px;
	}

	.xxxprodbannermobinfo {
		font-size: 12px;
	}

	.xxxprodbannermobinfo p {
		margin: 0 0 5px 0;
		padding: 0;
		line-height: 1;
	}

	.xxxprodbannermobinfo p > span {
		display: inline-block;
		white-space: nowrap;
	}

	.xxxprodbannermobinfo p:last-child {
		margin-bottom: 0;
	}

	.xxxprodbannermobprice {
		display: block;
		font-weight: bold;
		font-size: 50px;
		/*color: orangered;*/
		color: #662d91;
		margin: 0;
		padding: 0;
		line-height: 1;
		min-width: min-content;
		max-width: max-content;
	}

	.xxxprodbannermobprice:first-letter {
		font-size: 0.6em;
		line-height: 1.4;
		margin-left: -0.2em;
		padding: 0 0 1em 0;
		vertical-align: bottom;
	}

	.xxxprodbannermoblegal {
		font-size: 10px;
	}

	.xxxprodbannermoblegal p {
		margin: 0;
		padding: 0;
		line-height: 1;
	}

	.xxxprodbannermob .xxxopenchannellist {
		display: inline-block;
		text-align: center;
		padding: 0;
		margin: 0;
		position: relative;
		max-width: 100%;
		vertical-align: middle;
	}

	.xxxprodbannermob .xxxopenchannellist p {
		display: inline-block;
		background: rgb(182, 182, 182);
		border-radius: 5px;
		font-weight: bold;
		padding: 0 0.8em;
		position: relative;
		vertical-align: middle;
		margin: 0;
		font-size: 3vw;
	}

	.xxxprodbannermob .xxxopenchannellist:hover > p {
		background: rgb(139, 139, 139);
		cursor: pointer;
	}

	.xxxprodbannermob .xxxbuttonget {
		width: 100%;
		position: relative;
		bottom: 0;
		font-size: 3vw;
		margin: 0;
		padding-right: 4px;
		padding-left: 4px;
	}
}

/* END Versión mobile */

/* Con video */
.videocentered {
	position: absolute;
	left: -50%;
	width: 200%;
	height: 100%;
	top: 0;
}
.xxxprodbannermob .videocentered {
	height: calc(100% - 114px);
}
.videocentered video {
	position: absolute;
	top: 0;
	bottom: 0;
	right: 0;
	left: 0;
	margin: auto;
	height: 100%;
	width: auto;
}
.videocentered + a.bglink {
	background: transparent;
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
}
/* END Con video */

/* END bannerdeproducto */
.sucursaleslinks, .coberturalinks {
	width: 100%; /* Si no soporta calc() */
	width: calc(100% + 12px);
	margin: 6px 0 0 -6px;
	display: table;
}

.sucursalesrow {
	display: table-row;
}

.sucursalescell {
	display: table-cell;
	width: 50%;
	height: 100%;
	padding: 6px;
	margin: 0;
	position: relative;
}

.brickwrapper {
	display: block;
	width: 100%;
	height: 100%;
	position: absolute;
	top: 0;
	left: 0;
	padding: 6px;
}

.bricksucursal {
	display: inline-block;
	margin: 0;
	padding: 0;
	width: 100%;
	height: 100%;
	vertical-align: top;
	cursor: pointer;
	position: relative;
	/*background: #F3F3F3;*/
	background: #FAFAFA;
	color: #337ab7;
}

.selectedsuc {
	/*border: 1px solid;*/
	/*background: #E9E9E9;*/
	background: #EAEAEA;
}

.bricksucursal:hover, .bricksucursal:focus {
	color: #23527c;
	text-decoration: none;
}

.sucursalwrapper {
	min-height: 80px;
	padding: 1em;
}

.bricksucursal span {
	display: block;
	min-height: 1.5em;
}

span.localidad {
	display: inline;
	padding: 0;
	margin: 0;
}

.bricksucursal span.versucursal {
	text-decoration: underline;
	background: url(../img/icono_geo.png) no-repeat -3px center;
	background-size: 1em 1em;
	padding: 0 0 0 1em;
	display: inline-block;
}

.versucursal img {
	display: inline-block;
	height: 1em;
	width: 1em;
	margin-right: 3px;
}

#sucursalmapa iframe {
	width: 100%;
}

span.sucursalbarrio {
	font-weight: bold;
	display: inline-block;
}

@media (max-width: 320px) {
	.sucursalescell {
		display: block;
		width: 100%;
	}
}

/* Switch */

h1.switch {
	cursor: pointer;
}

h1.switch + h1.switch {
	float: right;
}

.selectorball {
	width: 10px;
	height: 10px;
	background: rgb(0, 143, 226);
	display: none;
	vertical-align: middle;
	border-radius: 50%;
	margin: 0 7px 0 0;
}

.off .selectorball {
	/* background: rgb(207, 207, 207); */
	background: rgb(235, 235, 235);
}

.legales {
	padding: 14px;
}

@media (max-width: 699px), (orientation: portrait) {
	h1.switch + h1.switch {
		float: none;
		display: block;
	}

	.selectorball {
		display: inline-block;
	}
}

h1.switch.off {
	/* opacity: .5; */
	color: rgb(207, 207, 207);
}

.switched.on {
}

.switched.off {
	display: none;
}

/* END Switch */
iframe {
	width: 100% !important;
	max-width: 100% !important;
	display: block;
}/* internet packs */
/* Estilos para armar en bricks los distintos combos de internet */
.xxxinternetbrickscontainer {
	width: 100vw;
	width: calc(100% + 24px);
	max-width: 1080px;
	margin: 0 0 20px -12px;
	padding: 0;
	display: block;
	text-align: left;
}

.xxxintbrickwrap {
	width: 25%;
	padding: 12px 12px 24px 12px;
	display: inline-block;
}

@media (max-width: 910px) and (min-width: 661px) {
	.xxxintbrickwrap {
		width: 33.33333333333333%;
	}
}

@media (max-width: 660px) and (min-width: 351px) {
	.xxxintbrickwrap {
		width: 50%;
	}
}

@media (max-width: 350px) {
	.xxxintbrickwrap {
		width: 100%;
	}
}

.xxxinternetbrick {
	width: 100%;
	margin: 0;
	padding: 5px;
	display: inline-block;
	text-align: center;
	background: white;
	border: 1px solid silver;
	border-radius: 10px;
}

.xxxinternetbrick span {
	display: block;
	margin: 5px auto;
	width: 100%;
}

.xxxinternetbrick header {
	width: 100%;
	height: 60px;
	display: block;
	margin: 5px auto;
}

.xxxinternetbrick header img {
	position: relative;
	height: 100%;
	display: block;
	margin: 0 auto;
}

.xxxinternetlabel {
	margin: 5px 0 -5px 0;
	font-weight: normal;
	text-transform: uppercase;
	display: block;
	font-size: 11px;
}

.xxxinternetlabel.extra {
	font-size: 14px;
	margin: 7px auto 11px auto;
	line-height: 0;
}
/* nuevos combos de productos */
.xxxinternetlabel.extra.comboinfo {
	margin: 10px auto 5px auto;
	line-height: 0.6;
}

.xxxinternetbrick span.xxxopenchannellist {
	display: inline;
	border-radius: 5px;
	font-weight: bold;
	padding: 0 0.8em;
	margin: 5px 0;
	width: 135px;
	max-width: 100%;
	font-size: 14px;
	color: black;
	cursor: pointer;
	line-height: 2;
	background: #fff;
	border: 1px solid rgb(182, 182, 182);
}

.xxxinternetbrick span.xxxopenchannellist:hover {
	background: rgb(139, 139, 139);
	cursor: pointer;
}

.xxxinternetbrick header + .xxxinternetlabel {
	margin-top: 10px;
}

.xxxinternetprice {
	font-size: 3em;
	color: #662D91;
	font-weight: bold;
	line-height: 0.8;
	height: 0.8em;
	display: block;
	margin: 5px auto;
}

.xxxinternetlegal {
	font-size: 11px;
}

.xxxinternetbrick .xxxbuttonget {
	margin-bottom: -26px;
	position: relative;
}

/* products bricks */
.products-bricks.xxxinternetbrickscontainer {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
	gap: 36px 24px;
	align-items: stretch;
	padding: 0 12px;

	& .xxxintbrickwrap {
		padding: 0 !important;
		width: 100% !important;

		& .xxxinternetbrick {
			display: grid;
			grid-template-rows: 1fr auto;
			height: 100%;

			& header {
				height: 90px;
			}

			& .secondary-button-link {
				text-decoration: none !important;
				color: inherit !important;
			}

			& .top-info {
			}

			& .bottom-info {
				& .xxxinternetlegal {
					min-height: 1em;
					min-height: 15px;
				}
			}
		}
	}
}
/* END products bricks */
/* END internet */
/* Faqs */
/* Estilos para las secciones de preguntas y respuestas, incluyendo acordeón */

.xxxfaqs {
	list-style: none;
	margin: 0;
	padding: 0;
	padding-top: 10px;
}

.xxxfaqsquestion {
	cursor: pointer;
	padding: 0;
	margin: 0;
	position: relative;
}

.xxxfaqsquestion.witharrow {
	padding-left: 1em;
	padding-left: calc(1em + 6px);
}

.xxxfaqs li + li .xxxfaqsquestion {
	margin-top: 14px;
}

.xxxfaqsquestionarrow {
	content: "";
	background: transparent;
	width: .5em;
	height: .5em;
	display: none; /* por js se muestra como inline-block */
	margin-bottom: .2em;
	margin-left: -1em;
	margin-right: 0.5em;
	margin: 0 .5em .2em 0;
	border-right: 2px solid;
	border-bottom: 2px solid;
	-webkit-transform: rotate(45deg);
	-ms-transform: rotate(45deg);
	transform: rotate(45deg);
	position: absolute;
	left: 2px;
	top: .25em;
}

.accordionlisttoclose .xxxfaqsquestionarrow {
	-ms-transform: rotate(225deg);
	-webkit-transform: rotate(225deg);
	transform: rotate(225deg);
	top: .5em;
}

.xxxfaqsanswer {
	display: block;
	padding-left: 3rem;
	font-size: 16px;
	padding-top: 12px;
}

/* END Faqs *//* FaqsPlus */
/* Estilos para las secciones de preguntas y respuestas, incluyendo acordeón */
/* Extilos extendidos, aplican a los títulos de grupos de preguntas */

/* Gruposaccordion*/
.pagetitle.witharrow {
  padding-left: 30px;
  position: relative;
  cursor: pointer;
}

.pagetitle h1 .xxxfaqsquestionarrow {
  margin: 0;
  padding: 0;
  left: 3px;
  right: 0;
  margin-top: 3px;
}

.pagetitle.witharrow + .xxxfaqs {
  margin-left: 30px;
}

.GRUPOaccordionlisttoclose  .xxxfaqsquestionarrow {
  -ms-transform: rotate(225deg);
  -webkit-transform: rotate(225deg);
  transform: rotate(225deg);
  top: .5em;
}.tablestyle {
	display: table;
}

.tablerow {
	display: table-row;
}

.tablecolumn {
	display: table-column;
}

.tablecell {
	display: table-cell;
}

.table {
	display: table;
}

.table.bigtable {
	width: 100%;
}

.row {
	display: table-row;
}

.row:nth-child(even) {
	/*background: rgba(0, 0, 0, 0.05);*/
}

.cell {
	display: table-cell;
	vertical-align: top;
	padding: 5px 20px;
}

.datecell {
	width: 11em;
	text-align: right;
	vertical-align: middle;
}

.namecell {
}

.rowhead {
	background-color: rgba(184, 184, 184, 0.99);
}

.labelinfocell {
	display: none;
}

.atmail {
	display: inline-block;
}


@media (max-width: 699px) {
	.row {
		display: table-row;
		/*display: block;*/
		padding: 15px 0;
	}

	.bigtable .rowhead {
		display: none;
	}

	.bigtable .row {
		text-align: center;
	}

	.bigtable .labelinfocell {
		display: inline;
		font-weight: bold;
		font-size: .85em;
	}

	.bigtable .cell {
		display: inline-block;
		width: 48%;
		text-align: left;
	}
	
	.namecell, .bigtable .cell.namecell {
		display: block;
		width: 100%;
		margin: 0 auto;
		font-weight: bold;
		padding-left: 25px;
	}
	
	.datecell {
		display: block;
		text-align: center;
		width: 100%;
	}
	
	.celltexto {
		border-left: 1px solid silver;
	}
}

@media (max-width: 499px) {
	.celltexto {
		display: block;
		width: 100%;
		border-left: 0;
		border-top: 1px solid silver;
		border-bottom: 1px solid silver;
	}
	
	.cell div {
		display: inline-block;
		max-width: 100%;
	}

	.bigtable .labelinfocell {
		display: inline-block;
		width: 100%;
	}
}
.socialintabla-icon {
	display: inline-block;
    height: 26px;
    width: 26px;
    border-radius: 50%;
    background-color: #babab9;
    background-size: cover;
    background-position: center;
    vertical-align: sub;
    margin: 0 9px 0 0;
    padding: 0;
}
.socialintabla-icon.facebook {
    background-color: #3b5998;
    background-image: url(/img/icono_facebook.svg);
}
.socialintabla-icon.twitter {
    background-color: #55ACEE;
    background-image: url(/img/icono_twitter.svg);
}
.form {
	width: 500px;
	max-width: 100%;
	margin: 0 0 0 -12px;
	padding: 0;
}

form {
	background: transparent;
	display: block;
	width: 500px;
	max-width: 100%;
	margin: 0;
	padding: 0;
	min-width: 100%;
}

form.forminpage, form.forminpage + div form {
	margin: 0 auto;
	text-align: center;
}

#modalcontent form {
	width: 512px;
	max-width: 100%;
	max-width: calc(100% + 24px);
}

.form .modalsubtit {
	text-align: center;
}

.form.clientdataloaded .modalsubtit {
	text-align: inherit;
}

.clientinput + .basesycondiciones {
	padding-top: 0;
	padding-bottom: 0;
	margin-top: -10px;
}

.inputwrapper {
	display: inline-block;
	vertical-align: top;
	width: 50%;
	padding: 12px;
	margin: 10px 0 20px 0;
	border: none;
}

@media (min-width: 701px) {
	form.condireccion .inputwrapper#inputtel,
	form.condireccion .inputwrapper#inputlocalidad {
		/*width: 25%;*/
	}
}
@media (min-width: 441px) and (max-width: 700px) {
	form.condireccion .inputwrapper#inputtel,
	form.condireccion .inputwrapper#inputlocalidad {
		width: 50%;
	}
	form.condireccion .inputwrapper#inputdireccion {
		/*width: 100%;*/
	}
}

.inputwrapper.groupinputs {
	padding-right: 0;
	padding-left: 0;
}

.groupinputs .inputwrapper {
	margin: 0;
	padding-top: 0;
	padding-bottom: 0;
	width: 33.3333333%;
}

.inputwrapper.clientinput {
	margin-right: auto;
	margin-left: auto;
	display: block;
}

.inputwrapper.clientinput.clientdataloaded {
	margin-right: inherit;
	margin-left: inherit;
	display: inherit;
}

.submitwrapper .inputwrapper, #concursoformwrapper .submitwrapper .inputwrapper {
	display: block;
	margin-right: auto;
	margin-left: auto;
}

#modalcontent .form p.textinmodal {
	width: 100%;
	display: block;
	margin-left: 12px;
}

form .legales {
	width: 100%;
	margin: 12px 0;
	color: #333;
}

form .xxxbuttonget.xxxbutton, .form .xxxbuttonget.xxxbutton {
	width: 100%;
	font-size: 21px;
}

@media (min-width: 540px) {
	.xxxprodbannerdesk+form .xxxbuttonget.xxxbutton {
		min-width: 190px;
		width: auto;
		max-width: 100%;
		display: block;
		margin: 0 auto;
	}
}

@media (max-width: 440px) {
	.inputwrapper {
		width: 100%;
	}

	form .xxxbuttonget.xxxbutton, .form .xxxbuttonget.xxxbutton {
		width: 100%;
	}
}

.inputtitle {
	display: block;
	width: 100%;
	line-height: 1;
	text-align: left;
	color: rgb(0,86,148);
	margin: 0 0 10px 0;
	font-weight: bold;
}

.inputtext, .inputwrapper select {
	display: inline-block;
	margin: 0;
	background: white;
	width: 100%;
	max-width: 100%;
	min-height: 37px;
	padding: 0 5px;
	border-radius: 5px;
	border: thin silver solid;
	line-height: 2.5;
	font-size: 14px;
	color: rgb(0,86,148);
	text-align: left;
}

textarea.inputtext {
	min-height: 70px;
}

.inputwrapper select {
	height: 37px;
	line-height: 2;
	-webkit-appearance: none;
	-moz-appearance: none;
	text-indent: 1px;
	text-overflow: '';
	vertical-align: top;
}

.inputwrapper select::-ms-expand {
	display: none;
}

.inputwrapper.dni select {
	width: 50px;
	border-right: 0;
	border-top-right-radius: 0;
	border-bottom-right-radius: 0;
	background-image: url('../img/flecha-01.svg');
	background-repeat: no-repeat;
	background-size: 10px;
	background-position: calc(100% - 10px) center;
}

.inputwrapper.dni input.inputtext {
	width: calc(100% - 50px);
	border-top-left-radius: 0;
	border-bottom-left-radius: 0;
}

.submitwrapper, .inputtextareawrapper{
	display: block;
	border-color: rgb(32, 139, 32);
	color: rgb(5, 58, 5);
}

.inputtextareawrapper, .inputcheckwrapper {
	width: 100%;
	padding: 0 12px;
}

.submit {
	display: block;
	margin: 0;
	background: white;
	width: 100%;
	max-width: 100%;
	padding: 0 5px;
	border-radius: 5px;
	border: 2px solid rgb(0,86,148);
	line-height: 2.5;
	font-size: 14px;
	color: rgb(0,86,148);
	font-weight: bold;
}

form .pagetitle {
	padding: 6px 0 14px 0;
	margin: 12px;
}

#concursoformwrapper .inputwrapper {
	margin: 0;
}

.triviawrapper {
	display: block;
	width: 100%;
	padding: 0 12px;
}

.preguntatrivia {
	margin-top: 10px;
	display: block;
}

.respuesta {
	font-weight: normal;
}

.speechpreform {
	display: block;
	text-align: center;
	margin: 10px auto;
}

/* Grupo de botones de servicios */
.checkButtonsGroupWrapper {
	display: block;
	width: 100%;
	text-align: left;
}

.checkButtonsWrapper {
	display: inline-block;
	width: 85px;
	max-width: 30%;
}

.checkButtonsWrapper + .checkButtonsWrapper {
	margin-left: 10px;
}

.checkButton input {
	display: none;
}

.checkButton {
	border: 1px solid silver;
	border-radius: 5px;
	padding: 10px;
	width: 100%;
	min-height: 80px;
	position: relative;
	cursor: pointer;
}

.checkButton:active {
	box-shadow: 0 0 3px rgba(0, 0, 0, 0.4) inset;
}

.checkButton .checklight {
	width: 10px;
	height: 10px;
	border-radius: 50%;
	display: block;
	background: url("link_to_image");
	background-color: #ABB1A8;
	position: absolute;
	right: 10px;
	top: 10px;
}

.checkButton input:checked + .checklight {
	background: url("link_to_another_image");
	background-color: #2DB018;
}

.servicesignwrapper {
	width: 80%;
	width: calc(100% - 20px);
	height: 100%;
	display: block;
	position: relative;
}

.checkButton img {
	opacity: .5;
	width: 46%;
	position: absolute;
	left: 10px;
	top: 10px;
}

.checkButton input:checked + .checklight + img, .checkButton input:checked + .checklight + .servicesignwrapper img {
	opacity: 1;
}

.checkButton .servicesignwrapper > img {
	position: relative;
	display: block;
	width: 100%;
	left: 0;
	top: 0;
}

.servicename {
	position: absolute;
	bottom: 10px;
	left: 10px;
	font-weight: normal;
	line-height: 1;
}

.servicesignwrapper .servicename {
	position: relative;
	display: block;
	left: 0;
	bottom: 0;
	margin: 5px auto 0 auto;
	width: 100%;
	text-align: center;
}

/* END Grupo de botones de servicios */
/* Social in top */
@media (min-width: 700px) and (orientation: landscape) {
	.barratop {
		display: block;
		height: 80px;
		width: 100%;
		background: white;
		top: 0;
		margin: 0 auto;
	}

	#socialarriba ul {
		list-style: none;
		display: block;
		float: right;
		margin: 0;
		padding: 0;
		padding-top: 15px;
		position: relative;
		z-index: 1;
	}

	#socialarriba ul li {
		display: inline-block;
		margin: 0;
		padding: 0 0 0 20px;
		vertical-align: middle;
		height: 20px;
	}

	#socialarriba ul li a {
		display: block;
		height: 30px;
		width: 30px;
		background: rgb(186, 186, 185);
		padding: 0;
		margin: 0;
		border-radius: 50%;
	}

	#socialarriba ul li a:hover { /* :hover bg genérico */
		background: rgb(139, 139, 139);
	}

	#socialarriba ul li a.Facebooklink:hover { /* :hover bg facebook */
		background: #3b5998;
	}

	#socialarriba ul li a.Twitterlink:hover { /* :hover bg twitter */
		background: #55ACEE;
	}

	#socialarriba ul li a.Teléfonolink:hover { /* :hover bg phone */
		background: #6AB26A;
	}

	#socialarriba ul li a.Maillink:hover { /* :hover bg mail */
		background: #ECB507;
	}

	#socialarriba ul li a.Ayudalink:hover { /* :hover bg mail */
		background: #FF930B;
	}

	#socialarriba ul li a.YouTubelink:hover { /* :hover bg youtube */
		background: #CD201F;
	}

	#socialarriba ul li a img {
		display: block;
		margin: 0 auto;
		padding: 0;
		width: 100%;
		height: 100%;
	}
}

@media (max-width: 699px), (orientation: portrait) {
	.barratop {
		display: none;
	}
}

/* END Social in top */

/* Social in footer */
#socialinfooter {
	width: 200px;
	display: inline-block;
	margin: 0;
	padding: 0;
	vertical-align: top;
}

#socialinfooter ul {
	list-style: none;
	padding: 0;
	margin: 0;
}

#socialinfooter li {
	list-style: none;
	padding: 0;
	margin: 0 0 1.1em 0;
}

#socialinfooter a {
	color: inherit;
}

#socialinfooter img {
	width: 2em;
	background: rgb(186, 186, 185);
	border-radius: 50%;
}

#socialinfooter a:hover img {
background: rgb(139, 139, 139);
}

#socialinfooter a.Facebooklink:hover img, #socialbottom a.Facebooklink:hover img, #socialinmenu a.Facebooklink:hover img { /* :hover bg facebook */
	background: #3b5998;
}

#socialinfooter a.Twitterlink:hover img, #socialbottom a.Twitterlink:hover img, #socialinmenu a.Twitterlink:hover img { /* :hover bg twitter */
	background: #55ACEE;
}

#socialinfooter a.Teléfonolink:hover img, #socialbottom a.Teléfonolink:hover img, #socialinmenu a.Teléfonolink:hover img { /* :hover bg phone */
	background: #6AB26A;
}

#socialinfooter a.Maillink:hover img, #socialbottom a.Maillink:hover img, #socialinmenu a.Maillink:hover img { /* :hover bg mail */
	background: #ECB507;
}

#socialinfooter a.Ayudalink:hover img, #socialbottom a.Ayudalink:hover img, #socialinmenu a.Ayudalink:hover img { /* :hover bg mail */
	background: #FF930B;
}

#socialinfooter a.YouTubelink:hover img, #socialbottom a.YouTubelink:hover img, #socialinmenu a.YouTubelink:hover img { /* :hover bg youtube */
	background: #CD201F;
}

#socialinfooter img, #socialinfooter span {
	display: inline-block;
	margin: 0 0 0 12px;
}

span.negrita {
	font-weight: bold;
	color: rgb(133, 133, 133);
	font-size: 1.2em;
	vertical-align: bottom;
}

@media (min-width: 700px) and (orientation: landscape) {
}
/* END Social in footer */


/* Social in bottom */

@media (min-width: 799px) {
	#socialbottom {
		display: none;
	}
}
/* END Social in bottom */

/* SocialLinks en menú mobile */
#socialinmenu {
	display: none;
}

@media (max-width: 699px), (orientation: portrait) {
	#socialinmenu {
		display: block;
		position: absolute;
		left: -233px;
		/*top: calc(100vh - 92px - 47px);*/
		top: 100%;
		width: 233px;
		height: 47px;
		overflow: hidden;
		background: rgb(233, 233, 233);
		padding: 0;
		margin: -47px 0 0 0;
		text-align: center;
		z-index: 9999;
	}

	#menuwrapper .blockcontainer {
		height: 100%;
		width: 100%;
	}

	#socialinmenu .blockcontainer {
		padding: 12px;
	}

	#socialinmenu ul {
		width: 100%;
		height: 100%;
		list-style: none;
		margin: 0;
		line-height: 0;
		padding: 0;
	}

	#socialinmenu ul li {
		width: 20%;
		height: 100%;
		padding: 0;
		margin: 0;
		list-style: none;
		display: inline-block;
	}

	#socialinmenu ul li:hover {
		color: inherit;
		background: inherit;
	}

	#socialinmenu img {
		background: rgb(186, 186, 185);
		border-radius: 50%;
	}
}

/* END SocialLinks en menú mobile */
.mediosdepago {
	text-align: center;
	width: 100%;
	display: block;
}

.mediosdepago .mediosdepagoblock {
	display: inline-block;
	margin: 12px;
	width: 118px;
	height: 118px;
	vertical-align: baseline;
}

.mediosdepagoblock img {
	width: 100%;
	height: 100%;
}/* Estilos para el buscador y resultados de búsqueda */

.inputwrapper.buscador {
	position: relative;
	width: 65%;
	margin: 10px 0;
}

.selctwitharrow .inputtext {
	padding-right: 32px;
}

.selctwitharrow .before/*, .selctwitharrow:before*/ {
	content: "";
	display: block;
	position: absolute;
	right: 25px;
	border-right: 5px solid transparent;
	bottom: calc(50% - 1px);
	background-color: transparent;
	width: 7px;
	height: 7px;
	border: 1px solid rgb(134, 133, 133);
	border-top: 0;
	border-left: 0;
	-webkit-transform: rotate(45deg);
	-ms-transform: rotate(45deg);
	transform: rotate(45deg);
	z-index: 0;
}

.selctwitharrow .after/*, .selctwitharrow:after*/ {
	content: "";
	display: block;
	position: absolute;
	background-color: transparent;
	width: 32px;
	margin: 0;
	padding: 0;
	height: 35px;
	top: 13px;
	right: 13px;
	border-radius: 0 4px 4px 0;
	border-left: 1px solid silver;
	cursor: pointer;
}

.selctwitharrow.active .after/*, .selctwitharrow.active:after, .selctwitharrow:active:after*/ {
	box-shadow: 0 1px 5px rgba(0, 0, 0, 0.26) inset;
	border-bottom-right-radius: 0;
}

.active .inputtext, .openlist .inputtext {
	border-bottom-right-radius: 0;
	border-bottom-left-radius: 0;
}

.inputwrapper {
	margin: 10px 0;
}

.presearch .xxxfaqsquestion {
	font-size: 16px;
}

.presearch {
	border: 1px solid silver;
	border-top: 0;
	border-radius: 0 0 5px 5px;
	display: none; /* por js se pasa a display: block */
	width: 86%;
	left: 7%;
	width: calc(100% - 34px);
	left: calc(17px);
	min-height: 2em;
	padding: 5px;
	margin: 0 auto;
	position: absolute;
	background: white;
	z-index: 1;
}

#buscadorwrapper {
	width: 100%;
	display: block;
	background: rgba(0, 0, 0, 0.05);
}

#buscadorwrapper #searchform {
	margin: 0;
	width: 50%;
}

#buscadorwrapper .submitwrapper {
	display: inline-block;
	width: 35%;
}

#buscadorwrapper .submitwrapper .inputwrapper {
	width: 100%;
}

#buscadorwrapper .xxxbuttonget.xxxbutton {
	line-height: 37px;
	padding: 0;
}

#searchresults {
	display: block;
	margin: 12px 0;
	padding: 0;
}

.xxxfaqsquestion.sinresultados {
	cursor: auto;
}

.sinencontrar {
	margin: 30px 0;
}

.sinencontrar a {
	text-decoration: underline;
	display: inline-block;
}

#searchresults .pagetitle {
	margin: 30px 0 10px 0;
	padding: 0;
}

.presearch .xxxfaqs {
	padding: 0;
}

.prequestion {
	font-size: 16px;
	margin: 15px 0;
	padding: 0 10px;
	cursor: pointer;
}

.vermasbutton {
	background: rgba(0, 0, 0, 0.05);
	padding: 5px;
	text-align: center;
	margin: 10px 0 0 0;
	font-size: 16px;
	cursor: pointer;
}

.preselected {
	text-decoration: underline;
}

@media (max-width: 1000px) {
	#buscadorwrapper #searchform {
		width: 60%;
	}
}

@media (max-width: 900px) {
	#buscadorwrapper #searchform {
		width: 65%;
	}
}

@media (max-width: 800px) {
	#buscadorwrapper #searchform {
		width: 70%;
	}
}

@media (max-width: 600px) {
	#buscadorwrapper #searchform {
		width: 100%;
	}
}

@media (max-width: 375px) {
	.inputwrapper.buscador {
		width: 70%;
		padding: 12px 0 12px 12px;
	}

	#buscadorwrapper .submitwrapper {
		width: 30%;
	}

	#buscadorwrapper .submitwrapper .inputwrapper {
		padding: 12px 12px 12px 0;
	}

	#buscadorwrapper .submitwrapper .inputwrapper .xxxbuttonget.xxxbutton {
		font-size: 14px;
		font-weight: normal;
		border-top-left-radius: 0;
		border-bottom-left-radius: 0;
	}

	.inputwrapper.buscador .inputtext {
		border-top-right-radius: 0;
		border-bottom-right-radius: 0;
		border-right-width: 0;
	}
}


			/*END SelectList & Google maps*/
			#map-canvas {
				position: relative;
				display: block;
				margin: 0;
				padding: 0;
				width: 100% !important;
				height: 300px;
				height: 400px;
				max-height: 60vh;
				/*max-height: calc(25vh + 15vw);*/
				margin: 10px 0 0 0;
				background-color: #EAEAEA;
			}

			#map-canvas .loading {
				position: absolute;
			}

			/* Estilos a la lista autocompletar de Google */
			.pac-container:after {
				display: none;
			}

			.pac-container {
				box-shadow: none;
				border: 1px solid silver;
				border-top: 0;
				border-radius: 0 0 4px 4px;
				box-sizing: content-box;
				background-color: #fff;
				z-index: 0;
			}

			.pac-icon, .pac-icon-marker {
				display: none;
			}

			.pac-item:hover, .pac-item-selected {
				background-color: #FAFAFA;
			}

			.pac-item {
				color: rgb(51, 51, 51);
				font-family: 'Open Sans Condensed', sans-serif;
				font-size: 14px;
				line-height: 20px;
				padding: 10px;
				cursor: pointer;
			}
			/* END Estilos a la lista autocompletar de Google */

			/*Select List*/
			.selectlistwrapper {
				display: none;
				position: absolute;
				left: 0;
				width: 100%;
				padding: 0 12px;
				margin: 0;
				z-index: 1;
			}

			.active .selectlistwrapper {
				display: block;
			}

			.selectlist {
				display: block;
				background-color: white;
				border: 1px solid silver;
				border-top: 0;
				border-radius: 0 0 4px 4px;
				left: 0;
				width: 100%;
				padding: 0;
				margin: 0;
				overflow: hidden;
			}

			.zonabutton {
				display: block;
				padding: 10px;
				margin: 0;
				width: 100%;
				cursor: pointer;
			}

			.zonabutton + .zonabutton {
				border-top: 1px solid rgba(192, 192, 192, 0.5);
			}

			.zonabutton:hover {
				background-color: #FAFAFA;
			}

			@media (max-width: 375px) {
				.selctwitharrow .after {
					right: 0px;
					border-radius: 0;
				}

				.selctwitharrow .before {
					right: 12px;
				}

				.active .selectlistwrapper {
					padding-right: 0;
				}
			}

			#map-canvas .xxxbuttonget {
				font-size: 12px;
				margin-top: 10px;
				width: 100%;
			}

			/* Marcadores Google Maps */
			.gm-style > div > div + div + div > div + div + div + div > div + div + div > div:first-child { /* infoWindow tail (flecha inferior) */
				display: none !important;
			}

			.gm-style-iw {
				color: rgb(51, 51, 51) !important;
				font-family: 'Open Sans Condensed', sans-serif !important;
				font-size: 14px !important;
				background-color: white;
				border: 1px solid silver;
				padding: 20px 10px 10px 10px;
				border-radius: 5px;
				top: 35px !important;
			}

			.closeinfowindow {
				border: 0 !important;
				background-color: rgba(226, 33, 33, 0.51) !important;
				position: absolute !important;
				top: 40px !important;
				right: 20px !important;
			}

			.servicioslistwrapper {
				text-align: center;
				display: block;
				width: 100%;
				margin: 10px 0 0 0;
				padding: 0;
			}

			.servicioslisticon {
				display: inline-block;
				height: 30px;
				width: 30px;
				background: transparent;
				padding: 0;
				margin: 0;
			}

			.servicioslisticon + .servicioslisticon {
				margin-left: 10px;
			}
			
			.servicioslisticon img {
				display: block;
				margin: 0 auto;
				padding: 0;
				width: 100%;
				height: 100%;
			}

			/* lista de servicios */
			.listserviceitem {
				text-align: left;
				line-height: 1;
				display: block;
				margin: 4px auto;
				width: 70px;
			}
			
			.listserviceitem .servicioslisticon {
				height: 15px;
				width: 15px;
				margin: 0 4px -2px 0;
			}
			/* END lista de servicios */
			/* END Marcadores Google Maps */



			/*END SelectList & Google maps*/

/* END Estilos para el buscador y resultados de búsqueda */
.imagespile {
	display: block;
	width: 100%;
	width: calc(100% + 24px);
	margin-left: calc(-12px);
}

.imagespile .listed.element {
	display: inline-block;
	width: 25%;
	margin-bottom: 22px;
}

.imagespile .element a {
	padding: 0 12px;
}

.buscador .element a, .buscadorficha .element a {
	/*height: 222px;*/
	/*max-height: 20vw;*/
	overflow: hidden;
}

@media (max-width: 1000px) {
	.buscador .element a, .buscadorficha .element a {
		/*max-height: 19vw;*/
	}
}

@media (max-width: 800px) {
	.buscador .element a, .buscadorficha .element a {
		/*max-height: 21vw;*/
	}
}

@media (max-width: 690px) {
	.buscador .element a, .buscadorficha .element a {
		/*max-height: calc(50vw - 34px);*/
		/*height: calc(50vw - 34px);*/
	}
}

.SlideCarousel .element a {
    height: 100%;
    max-height: 100%;
}

.imagespile .eventinfo {
	width: 100%;
	width: calc(100% - 24px);
	left: 0;
	left: calc(12px);
	/*height: 43px;*/ /*Para dos líneas de texto*/
	height: 63px;
}

.buscador .eventinfo, .buscadorficha .eventinfo {
	height: 69px;
	padding: 6px 0;
}

/* Transiciones */
/* slides laterales para pasar de un día a otro */
.imagespile.slideouttoright {
	position: relative;
	transform: translateX(100vw);
	opacity: 0;
	transition: transform .25s ease, opacity .25s ease;
}

.imagespile.slideouttoleft {
	position: relative;
	transform: translateX(-100vw);
	opacity: 0;
	transition: transform .25s ease, opacity .25s ease;
}

.imagespile.waiting {
	display: none;
}

.imagespile.waiting.slideintoright {
	position: relative;
	transform: translateX(-100vw);
	opacity: 0;
	display: block;
}

.imagespile.waiting.slideintoleft {
	position: relative;
	transform: translateX(100vw);
	opacity: 0;
	display: block;
}

.imagespile.slideintoright, .imagespile.slideintoleft {
	position: relative;
	transform: translateX(0);
	opacity: 1;
	transition: transform .25s ease, opacity .25s ease;
}
/* END Transiciones */

.eventlabel {
	background: #8E8E8E;
	display: block;
	position: absolute;
	width: auto;
	height: auto;
	top: 0;
	right: 12px;
	color: white;
	font-size: 14px;
	margin: 0;
	padding: 0 8px;
}

.eventlabel.viejo {
	background: #B7241C;
}

.eventlabel.ahora {
	background: #088032;
}

.eventlabel.futuro {
	background: #2272D6;
}

.progressbarwrapper {
	position: absolute;
	display: block;
	height: 3px;
	width: calc(100% - 24px);
	left: 12px;
	bottom: 0;
}

.progressbar {
	background: red;
	display: block;
	height: 100%;
	width: 0%; /* Se carga el % dinámicamente */
}

.imagenenslider > img {
	height: 100%;
}

@media (max-width: 578px) {
	.imagespile {
		width: calc(100% + 14px);
		margin-left: calc(-7px);
	}

	.imagespile .element a {
		padding: 0 7px;
	}

	.eventlabel {
		right: 7px;
	}

	.progressbarwrapper {
		left: 7px;
		width: calc(100% - 14px);
	}
	
	.imagespile .eventinfo {
		width: calc(100% - 14px);
		left: calc(7px);
	}
}

.element a.noimg img, .element a.noimg .img {
	background: transparent;
	padding: calc(50% - 60px) calc(50% - 40px) calc(50% - 20px) calc(50% - 40px);
	/*padding-top: calc(50% * 0.6775 - 37px);*/ /* Para dos líneas de texto */
	/*padding-bottom: calc(50% * 0.6775 - 0px);*/ /* Para dos líneas de texto */
	padding-top: calc(50% * 0.6775 - 36px);
	padding-bottom: calc(50% * 0.6775 + 25px);
	position: relative;
	width: 80px;
	height: 80px;
	box-sizing: content-box;
}

.element a.noimg .img {
	background: transparent;
	padding: 0;
	margin-top: calc(50% * 0.6775 - 36px);
	margin-bottom: calc(50% * 0.6775 + 25px);
	margin-left: auto;
	margin-right: auto;
	position: relative;
	width: 80px;
	height: 80px;
	box-sizing: content-box;
	background-size: contain;
	background-position: center;
	background-repeat: no-repeat;
}

.noimgbg {
	width: 89%;
	width: calc(100% - 24px);
	/*height: calc(100% - 43px);*/ /* Para dos líneas de texto */
	height: calc(100% - 69px);
	position: absolute;
	left: 0;
	margin: 0 0 0 12px;
	padding: 0;
	background: transparent;
	/*
	background: -webkit-radial-gradient(circle, white 35%, #F3F3F3 75%, #E8E8E8 120%);
	background: -o-radial-gradient(circle, white 35%, #F3F3F3 75%, #E8E8E8 120%);
	background: -moz-radial-gradient(circle, white 35%, #F3F3F3 75%, #E8E8E8 120%);
	background: radial-gradient(circle, white 35%, #F3F3F3 75%, #E8E8E8 120%);
	*/
	background: white;
	box-shadow: 0 0 80px #D8D8D8 inset;
}

@media (max-width: 578px) {
	.noimgbg {
		width: calc(100% - 14px);
		margin-left: 7px;
	}
}


	.eventinfo {
		background-color: rgba(0, 0, 0, 0.58);
		height: auto;
		position: absolute;
		width: 91%;
		bottom: 0;
		margin: 0;
		padding: 5px 0;
	}

	.element a .eventinfo span {
		display: block;
		position: relative;
		width: 100%;
		color: whitesmoke;
		font-size: 12px;
		text-overflow: ellipsis;
		overflow: hidden;
		white-space: nowrap;
		padding: 0 5px;
	}

	.element a .eventinfo span.channel.channelnum {
		padding: 6px 0 0 0;
		height: 22px;
	}

	.element a .eventinfo span span.channelnum {
		background: whitesmoke;
		display: inline-block;
		width: auto;
		color: #656565;
		height: 17px;
	}

	.imagespile .pagetitle {
		margin-left: 12px;
		margin-right: 12px;
	}

	/* Mobile */
	@media (max-width: 450px) {
		.ficha .pic {
			display: block;
			float: right;
			width: 35%;
			height: auto;
		}
		
		.tecdetails {
			display: table-cell;
			vertical-align: top;
			display: block;
		}

		.eventdetailgroup {
			display: table;
			width: 100%;
		}

		.pic + .tecdetails {
			padding-left: 0;
		}
	}

	@media (max-width: 690px) {
		.imagespile .listed.element {
			width: 50%;
		}

		.element a .eventinfo span {
			/*font-size: 10px;*/
		}
	}
	/* END Mobile */

/* Footer */
body.buscador #socialbottom, body.buscadorficha #socialbottom {
	display: block;
}
/* END Footer */

.element a > img {
	/*padding-bottom: 43px;*/ /* Para dos líneas de texto */
	padding-bottom: 69px;
}

	@media (min-width: 700px) and (orientation: landscape) {
		#menuwrapper {
			display: block;
			width: auto;
		}
		
		#menuwrapper > ul > li {
			display: inline-block;
			width: auto;
		}

		#menuwrapper > ul > li:hover, #menuwrapper > ul > li.hover, #menuwrapper > ul > li.tvsearchwrapper:hover > a {
			background: #005F97;
		}

	}

	.tvsearch {
		display: none;
		background-color: white;
		color: #333;
		font-size: 14px;
		font-weight: normal;
		text-align: left;
		width: 140px;
		height: 20px;
		padding: 0 10px;
		margin: 0;
		vertical-align: middle;
		border-radius: 10px;
		line-height: 20px;
		cursor: text;
		border: 0;
		text-transform: uppercase;
		background-color: transparent;
		color: whitesmoke;
		border-bottom: 1px solid whitesmoke;
		border-radius: 0;
		padding: 10px 0px;
		height: 40px;
		border-radius: 5px;
		color: #333;
		padding: 0 10px;
		background-color: white;
		vertical-align: top;
		margin-top: 10px;
	}

	.srchactive .tvsearch, .activatingsearch .tvsearch {
		display: inline-block;
	}

	.srchactive .tvsearch, .activatingsearch .tvsearch {
		width: 100%;
	}

	#menuwrapper > ul > li.tvsearchwrapper > a {
		min-width: auto;
		padding-right: 0;
	}

	#menuwrapper > ul > li.tvsearchwrapper.srchactive > a, #menuwrapper > ul > li.tvsearchwrapper.activatingsearch > a {
		padding-left: 36px;
		padding-right: 35px;
	}

	.opensearchbutton {
		display: inline-block;
		/*background: url('/img/lupa.svg') no-repeat transparent;*/
		background: url('/img/lupa_new-01.svg') no-repeat transparent;
		background-size: 30px 30px;
		width: 30px;
		height: 30px;
		vertical-align: middle;
		cursor: pointer;
		background-position: center;
	}

	.opensearchbutton:active {
		background-size: 27px 27px;
	}

	.srchactive .opensearchbutton, .activatingsearch .opensearchbutton {
		position: absolute;
		height: 100%;
		left: 0;
		cursor: default;
		background-size: 27px 27px;
		display: none;
	}

	#searchbuttoninmobile.opensearchbutton {
		display: none;
	}

	.tvsearchwrapper .closemodal {
		display: none;
	}

	.tvsearchwrapper.srchactive #closemodal, .tvsearchwrapper.srchactive .closemodal {
		display: block;
		top: 15px;
		right: 2px;
		background: transparent;
	}

	.tvsearchwrapper .closemodalcross {
		background: white;
		width: 6px;
		top: 10px;
		left: 8px;
		transition: width 3s ease, top 3s ease, left 3s ease;
	}

	.tvsearchwrapper.srchactive .closemodalcross, .tvsearchwrapper.srchactive .closemodalcross {
		width: 40px;
		height: 2px;
		top: 10px;
		left: -9px;
	}

	.closesearcher {
		background: transparent;
		position: absolute;
		width: 20px;
		height: 20px;
		display: block;
		right: 7px;
		z-index: 99999999;
		cursor: pointer;
		top: 20px;
		border-color: transparent;
		border-width: 10px;
		border-style: solid;
		transition: border-width .2s ease;
		visibility: hidden;
	}
	
	.srchactive .closesearcher {
		border-color: #005f97;
		border-width: 0px;
		visibility: visible;
	}

	.srchactive .closesearcher:active {
		border-width: 3px;
		transition: none;
	}

	.searchername.activesearchtv {
		display: none;
	}

	@media (max-width: 699px), (orientation: portrait) {
		/* Dentro del menú mobile */
		.tvsearch {
			width: 100%;
			border: 1px solid rgba(0, 0, 0, 0.15);
			display: none;
		}

		.activesearch .tvsearch {
			display: block;
		}

		li.tvsearchwrapper.srchactive > a, #menuwrapper > ul > li.tvsearchwrapper.srchactive > a {
			background: rgba(18, 18, 18, 0.28);
			background: #005F97;
			width: 100vw !important;
			padding: 0 60px 0 20px;
			top: 0;
			right: 0;
			left: 0;
			line-height: 60px;
		}

		.closesearcher {
			right: 0px;
			top: 0px;
			width: 55px;
			height: 60px;
		}

		.tvsearchwrapper.srchactive #closemodal, .tvsearchwrapper.srchactive .closemodal {
			top: 16px;
			right: 16px;
		}

		.searchername.activesearchtv {
			display: block;
			cursor: pointer;
		}

		.activesearch .searchername.activesearchtv, .opensearchbutton {
			display: none;
		}

		#searchbuttoninmobile.opensearchbutton {
			display: block;
			position: absolute;
			top: 0;
			height: 46px;
			right: 12px;
		}

		.activesearch .tvsearch.activesearchtv {
			display: inline-block;
		}

		.activesearch #menuwrapper {
			overflow: visible;
		}
	}
</style>

<style type="text/css">
	/* Selector temporal */
	.selctslide {
		display: table;
		width: 100%;
	}

	.selctslide ul {
		list-style: none;
		padding: 0;
		margin: 0;
		display: table-row;
	}
	
	.selctslide ul li {
		list-style: none;
		margin: 0;
		padding: 0;
		display: table-cell;
		width: 33.333333333333333333333333%;
		text-align: center;
		vertical-align: middle;
		cursor: pointer;
	}

	.selctslide ul li.unselected h1 {
		color: rgba(111, 111, 111, 0.46);
	}

	.selctslide ul li:hover h1 {
		color: #2491DA;
	}

	.selctslide ul li:active h1 {
		color: #054B79;
	}

	.selctslide ul li.unselected:hover h1 {
		color: rgba(169, 169, 169, 0.52);
	}

	.selctslide ul li.unselected:active h1 {
		color: rgba(169, 169, 169, 0.52);
	}

	.selctslide ul li.unselected:active h1 {
		color: rgba(88, 88, 88, 0.49);
	}

	.selctslide ul li:nth-of-type(1) {
		text-align: left;
	}

	.selctslide ul li:nth-of-type(3) {
		text-align: right;
	}

	.selctslide .pagetitle {
		padding-top: 6px;
		padding-bottom: 6px;
	}
	/* END Selector temporal */

	/* Buscador avanzado */
	li.tvsearchwrapper, li.tvsearchwrapper:hover {
		cursor: inherit;
	}

	li.tvsearchwrapper.srchactive, #menuwrapper > ul > li.tvsearchwrapper.srchactive {
		position: absolute;
		width: 0;
	}

	li.tvsearchwrapper.srchactive > a, #menuwrapper > ul > li.tvsearchwrapper.srchactive > a {
		display: block;
		top: 0;
		right: 100%;
		position: absolute;
	}

	@media (max-width: 699px), (orientation: portrait) {
		li.tvsearchwrapper.srchactive, #menuwrapper > ul > li.tvsearchwrapper.srchactive {
			width: 100vw;
			height: 60px;
			top: 0;
			background: red;
			z-index: 9999;
			right: 0;
		}

		li.tvsearchwrapper.srchactive:after, #menuwrapper > ul > li.tvsearchwrapper.srchactive:after {
			display: none;
		}

		li.tvsearchwrapper.srchactive > a > .tvsearch, #menuwrapper > ul > li.tvsearchwrapper.srchactive > a > .tvsearch {
			vertical-align: initial;
			height: 40px;
			border-radius: 5px;
			color: #333;
			padding: 0 10px;
			background-color: white;
		}
	}

	#menuwrapper > ul > li.tvsearchwrapper.srchactive:hover, #menuwrapper > ul > li.tvsearchwrapper.srchactive:hover > a {
		background: #005f97;
	}

	.buscadoravanzado {
		display: none;
	}

	body.buscador.activesearch {
		position: fixed;
		overflow: hidden;
		width: 100%;
	}

	.buscadoravanzado.srchactive {
		display: block;
		background-color: rgba(0, 0, 0, 0.83);
		background-color: white;
		position: absolute;
		left: 0;
		width: 100%;
		padding: 0;
		margin: 0;
		top: 60px;
		border-bottom: 1px solid black;
		border-bottom: 1px solid silver;
		box-shadow: 0 3px 5px rgba(0, 0, 0, 0.17);
		max-height: calc(100vh - 60px);
		overflow-y: hidden;
		overflow-x: hidden;
		z-index: 3;
	}

	@media (max-width: 699px), (orientation: portrait) {
	}
		.buscadoravanzado.srchactive {/*Estaba dentro del break "@media (max-width: 699px), (orientation: portrait)" */
			width: 100%;
			z-index: 9999;
			height: 100%;
			border: 0;
			box-shadow: none;
			left: 0;
		}

	.buscadoravanzado.srchactive .srchoptions {
		display: block;
		width: 100%;
	}

	/* Form dentro del panel */
	/* animación "cargando" */
	.searching {
		position: absolute;
		background-color: rgba(255, 255, 255, 0.81);
		top: 0;
		left: 0;
		width: 100%;
		height: 100%;
		z-index: 2;
	}

	@media (max-width: 699px), (orientation: portrait) {
		.searching {
			position: fixed;
		}
	}

	.searching .loading {
		position: absolute;
	}

	.searching.filtro .loading {
		position: fixed;
	}
	/* animación "cargando" */

	.srchoptions form, .srchoptions .form {
		width: 100%;
		text-align: left;
	}

	.srchoptions .form {
		width: 100%;
		margin-left: 0;
		position: relative;
	}

	.srchoptions .inputwrapper {
		width: 25%;
	}

	@media (max-width: 749px) {
		.srchoptions .inputwrapper {
			width: 50%;
		}
	}

	@media (max-width: 489px) {
		.srchoptions .inputwrapper {
			width: 100%;
		}
	}

	.catselectwrapper > select {
		width: 100%;
		border-radius: 5px;
	}

	#searchtvform .catselectwrapper select {
		margin: 1em 0;
	}

	@media (max-width: 699px), (orientation: portrait) {
		#searchtvform .catselectwrapper select {
			margin: 0;
		}

		.inputwrapper {
			margin: 0;
			/*padding-right: 0;*/
			/*padding-left: 0;*/
		}

		.submitwrapper .inputwrapper {
			/*margin: 10px 0;*/
			padding-right: 22px;
			padding-left: 22px;
		}
	}

	.inputtext {
		margin-top: 1em;
		margin-bottom: 1em;
	}


	/* Checkboxes */
	.check input {
		display: none;
	}

	.check label {
		display: block;
		width: 27px;
		height: 27px;
		margin: 3px 0 0 0;
		padding: 0;
		cursor: pointer;
		border-radius: 4px;
		border: 3px solid #005A96;
	}


	.check input:checked + label {
		background-image: url('/img/tildenaranja.svg');
		background-repeat: no-repeat;
		background-size: 17px 17px;
		background-position: center center;
	}
	/* END Checkboxes */

	/* check items */
	.checkitems {
		display: block;
		width: 400px;
		max-width: 100%;
		padding: 0;
		margin: 15px 0;
		white-space: nowrap;
	}

	.filterpremiumwrapper .checkitems, .filterhdwrapper .checkitems {
		text-align: right;
	}

	.checkitems:first-child {
		margin-top: 0;
	}

	.checkitems:last-child {
		margin-bottom: 0;
	}

	.checkitems span, .checkitems label {
		display: inline-block;
		padding: 0;
		margin: 0;
		vertical-align: bottom;
		line-height: 27px;
		color: rgb(0,86,148);
		font-weight: bold;
		cursor: pointer;
	}

	.checkitems .check {
		/*float: right;*/
		margin-left: 1em;
	}

	@media (max-width: 600px) {
		.checkitems {
			width: 100%;
		}
		
		.checkitems .check {
			float: right;
		}
	}

	.checkitems label {
	}
	/* END check items */


	/* datepicker */
	
	/* contenedor completo */

	.menuin .ui-datepicker {
		margin-left: -233px;
	}

	.ui-datepicker {
		display: none;
		background: white;
		width: 165px;
		text-align: center;
		border: 1px solid silver;
		border-radius: 5px;
		margin: 0 0 0 0;
	}
	
	/* Prev/next */
	.ui-datepicker-prev, .ui-datepicker-next {
		width: 30px;
		overflow: hidden;
		height: 1.5em;
		padding: 1px;
		cursor: pointer;
	}
	
	.ui-datepicker-prev {
		float: left;
	}
	
	.ui-datepicker-next {
		float: right;
	}
	/* END Prev/next */
	
	/* contenedor fechas y días de la semana */
	.ui-datepicker-calendar {
		width: 100%;
		margin: 4px 0 0 0;
	}
	
	/* días de la semana */
	.ui-datepicker-calendar thead {
		background: rgb(206, 213, 231);
		border-top: 1px solid rgb(181, 177, 216);
		border-bottom: 1px solid rgb(181, 177, 216);
	}
	
	.ui-datepicker-calendar th {
		text-align: center;
	}
	
	/* días de los meses previos o posteriores */
	.ui-datepicker-other-month {
		background: rgb(239, 239, 239);
	}
	
	/* día seleccionado */
	.ui-state-active {
		background: rgb(186, 209, 226);
		color: rgb(41, 94, 139);
		font-weight: bold;
		display: block;
	}

	/* no seleccionables */
	.ui-datepicker-unselectable.ui-state-disabled {
		background: rgb(239, 239, 239);
		cursor: default;
		opacity: .3;
	}
	/* END no seleccionables */
	
	/* END datepicker */
	/* END Form dentro del panel */

	/* Grupo de inputs */
	.inputsgroup {
		display: block;
		width: 100%;
		margin: 0;
		padding: 0;
	}

	.inputsgroup + .inputsgroup {
		border-top: 1px solid rgba(192, 192, 192, 0.55);
		padding: 20px 0;
	}

	.avanzadas {
		border-top: 1px solid rgba(192, 192, 192, 0.55);
		padding: 20px 0;
	}
	/* END Grupo de inputs */

	/* END Buscador avanzado */


	.grillatv select {
		padding: 0 10px;
		margin: 0 auto;
		display: block;
		cursor: pointer;
		-webkit-appearance: none;
		-moz-appearance: none;
		border: none;
		background: #404040;
		width: 100%;
		height: 100%;
		text-transform: uppercase;
		color: whitesmoke;
	}

	.grillatv select.onmobile {
		display: none;
		padding: 0 5px;
	}

	@media (max-width: 680px) {
		.grillatv select.onmobile {
			display: block;
		}

		.grillatv select.ondesk {
			display: none;
		}
	}

	@media (max-width: 699px), (orientation: portrait) {
	}
		.submitwrapper.atbottom { /* Estaba dentro del break "@media (max-width: 699px), (orientation: portrait)" */
			box-shadow: none;
		}
		form#searchtvform { /* Estaba dentro del break "@media (max-width: 699px), (orientation: portrait)" */
			background: transparent;
			display: block;
			/*width: 500px;*/
			max-width: 100%;
			margin: 0;
			padding: 0;
			position: absolute;
			top: 0;
			height: 100%;
			left: 0;
		}

		.inputsgroupswrapper { /* Estaba dentro del break "@media (max-width: 699px), (orientation: portrait)" */
			position: absolute;
			width: 100%;
			height: 10px;
			left: 0;
			top: 0;
			height: calc(100vh - 156px);
			overflow-y: auto;
		}

		#searchtvform .inputsgroupswrapper {
			padding: 0 10px;
		}

		.submitwrapper { /* Estaba dentro del break "@media (max-width: 699px), (orientation: portrait)" */
			display: block;
			border-color: rgb(32, 139, 32);
			color: rgb(5, 58, 5);
			position: absolute;
			width: 100vw;
			left: calc(50% - 50vw);
			top: calc(100vh - 156px);
			box-shadow: 0 0 25px rgba(0, 0, 0, 0.41);
		}

	@media (min-width: 1080px) {
		.submitwrapper {
			position: fixed;
			background: white;
			bottom: 0;
			top: calc(100vh - 86px);
		}
	}


/* Slides agrupados */
.slidersgroup {
	position: relative;
}

.slidersgroup .searcherblockwrapper.active {
	left: 0;
	opacity: 1;
}

.slidersgroup .searcherblockwrapper.inactive {
	position: absolute;
	top: 0;
	width: 100%;
	visibility: hidden;
	display: none;
}

.slidersgroup .searcherblockwrapper.toactivate {
	position: absolute;
	width: 100%;
	top: 0;
	/*opacity: .02;*/
}

.slidersgroup .searcherblockwrapper.toactivate.fromleft {
	left: 130%;
}

.slidersgroup .searcherblockwrapper.toactivate.fromright {
	left: -130%;
}

.slidersgroup .searcherblockwrapper.active.toleft {
	left: -110%;
	position: relative;
	opacity: .02;
	transition: left .5s, opacity .5s;
}

.slidersgroup .searcherblockwrapper.active.toright {
	left: 110%;
	position: relative;
	opacity: .02;
	transition: left .5s, opacity .5s;
}

.slidersgroup .searcherblockwrapper.toactivate.fromleft.activating, .slidersgroup .searcherblockwrapper.toactivate.fromright.activating {
	left: 0;
	opacity: 1;
	transition: all .5s;
}
/* END Slides agrupados */

/* Seleccionar pack */
.packpick {
	background: transparent;
	display: inline-block;
	height: 60px;
	vertical-align: middle;
	line-height: 60px;
	margin: 0 10px;
	padding: 5px 0 0 0;
	color: #8E8E8E;
	position: absolute;
	top: 0;
	z-index: 9999;
	margin-left: -85px;
	text-align: right;
}

.packpick span {
	background: transparent;
	display: block;
	height: 25px;
	line-height: 25px;
	font-size: 13px;
	margin: 0;
	padding: 0 5px;
	cursor: pointer;
	font-weight: bold;
}

.packpick input[type="checkbox"] + label span:last-child, .packpick input[type="checkbox"]:checked + label span:first-child {
	color: white;
}

.packpick input[type="checkbox"] + label span, .packpick input[type="checkbox"]:checked + label span:last-child {
	color: #8E8E8E;
}

@media (max-width: 699px), (orientation: portrait) {
	.packpick {
		position: relative;
		width: 233px;
		margin-left: 0;
		padding: 0;
		text-align: center;
		vertical-align: middle;
		height: 46px;
		line-height: 46px;
	}

	.packpick label {
		width: 100%;
		padding: 0 0 0 15px;
	}

	.packpick span {
		width: 50%;
		display: inline-block;
		text-align: left;
		font-size: 16px;
		vertical-align: middle;
	}

	.packpick input[type="checkbox"] + label span:last-child, .packpick input[type="checkbox"]:checked + label span:first-child {
		color: #181818;
	}

	.packpick input[type="checkbox"] + label span, .packpick input[type="checkbox"]:checked + label span:last-child {
		color: #8E8E8E;
	}

	.packpick input[type="checkbox"] + label span:last-child {
		border-left: 1px solid silver;
		text-align: center;
	}
}

.nav {
	text-align: right;
}

.packpick input {
	display: none;
}

.packpick label {
	display: inline-block;
	max-width: 100%;
	margin-bottom: 0;
	font-weight: normal;
}
/* END Seleccionar pack */

select.changingselect.hide {
	display: none;
}

/* Ficha en página */
.posterficha {
	display: block;
	width: 100%;
	position: relative;
}

.posterficha img {
	width: 100%;
}

.posterficha img + img {
	position: absolute;
	top: 0;
}

.buscadorficha .ficha.fichasection {
	width: 100%;
}

.buscadorficha .ficha h4.title {
	margin-bottom: 0;
	margin-top: 2px;
	padding-bottom: 0;
}

.info.ocurrencias > span.title {
	padding: 5px 0;
	color: #005A96;
}

.buscadorficha .info.ocurrencias span.daygroup {
	display: block;
	padding-left: 10px;
	border: none;
	font-weight: bold;
}

.pagetitle.msgmissingprogram {
	text-align: center;
	margin: 10px 0 25px 0;
	padding: 0;
}

.pagetitle.msgmissingprogram .h1 {
	font-size: 20px;
	text-transform: none;
	border-bottom: 1px solid silver;
	padding: 16px 10px 20px 10px;
}

.pagetitle.fichanoimg {
	padding-top: 14px;
	padding-bottom: 14px;
}

/* END Ficha en página */

/* En FichaTV */
.ficha .social {
	width: 100%;
	height: 30px;
	display: block;
	vertical-align: bottom;
	text-align: center;
	margin: 10px 0;
	padding: 20px 0 0 0;
	/*border-top: 1px solid silver;*/
	box-sizing: content-box;
}

.ratingwrapper {
	display: inline-block;
	margin-right: 20px;
	margin-top: 5px;
	margin-bottom: 5px;
	height: 13px;
	line-height: 13px;
	vertical-align: top;
	position: relative;
}

.ficha .social ul {
	list-style: none;
	display: block;
	margin: 0;
	padding: 0;
}

.ficha .social ul li {
	display: inline-block;
	margin: 0;
	padding: 0;
	vertical-align: middle;
	height: 30px;
}

.ficha .social ul li + li {
	margin-left: 20px;
}

.ficha .social ul li a {
	display: inline-block;
	height: 30px;
	width: 30px;
	background: rgb(186, 186, 185);
	padding: 0;
	margin: 0;
	border-radius: 50%;
	overflow: hidden;
}

.ficha .social ul li img {
	width: 100%;
}

.ficha .social ul li a.Facebooklink img {
	background-color: #3b5998;
}

.ficha .social ul li a.Twitterlink img {
	background-color: #55ACEE;
}

.ficha .social ul li a.WhatsApplink img {
	background-color: #25D366;
}

.ficha .social ul li a.Permalink img {
	background-color: #FF930B;
}
/* END En FichaTV */

/* Corrección de fuente en el menú */
/* por si no se carga muy rápido la fuente correcta */
#headermenucero {
    font-family: 'Open Sans Condensed', monospace;
}
/* END Corrección de fuente en el menú */

.packpicklist a.selected {
	position: relative;
}

.packpicklist a.selected:after {
	content: "";
	display: block;
	position: absolute;
	width: 10px;
	height: 10px;
	background: rgb(239, 122, 9);
	left: 3px;
	top: 50%;
	top: calc(50% - 5px);
	border-radius: 50%;
}

/* Grilla */
.searcherblockinner {
	position: relative;
}

.grillatv {
	border: 1px solid silver;
	overflow: hidden;
}

ul.listacanales {
	display: table;
	margin: 0;
	padding: 0;
	list-style: none;
	width: 100%;
}

.grillatv.chunico ul.listacanales {
	display: block;
	box-sizing: content-box;
}

ul.listacanales:before {
	content: "";
	display: none;
}

li.chrow {
	background: #E0E0E0;
	display: table-row;
	margin: 0;
	padding: 0;
	list-style: none;
	width: 100%;
}

/*
.grillatv.filtered li.chrow, .grillatv.premiumonly li.chrow, .grillatv.filtered.premiumonly li.chrow.pasa:not(.premium), .grillatv.filtered.premiumonly li.chrow.premium:not(.pasa) {
	display: none;
}

.grillatv.filtered li.chrow.pasa, .grillatv.premiumonly li.chrow.premium {
	display: inherit;
}
*/

.grillatv.filtered li.chrow {
	display: none;
}

.grillatv.filtered li.chrow.pasa {
	display: inherit;
}

.grillatv.chunico li.chrow {
	display: block;
}

ul.programslist {
	list-style: none;
	padding: 0;
	margin: 0 0 0 220px;
	display: block;
	width: 4320px;
	max-width: 4320px;
	height: 70px;
	overflow: hidden;
	white-space: nowrap;
}

.grillatv.chunico ul.programslist {
	width: 100%;
	height: auto;
	margin: 0;
	display: block;
}

li.program {
	height: 70px;
	list-style: none;
	background: white;
	margin: 0;
	padding: 0;
	display: inline-block;
	border-right: 1px solid silver;
	border-bottom: 1px solid silver;
	vertical-align: middle;
	cursor: pointer;
}

li.program.empty {
	background-color: #D0D0D0;
}

li.chrow:last-child ul.programslist li.program {
	border-bottom: none;
}

.grillatv.chunico li.program {
	display: block;
	width: 100% !important;
	max-width: 100% !important;
	padding: 0 !important;
	border-right: none;
	border-top: 1px solid silver;
}

.grillatv.chunico li.program:last-child {
	border-bottom: none;
}

li.program:hover {
	background-color: #D0D0D0;
	color: #505050;
}

li.program:last-child {
	border-right: none;
}

li.program .programwrapper {
	padding: 14px 10px;
	display: block;
	width: 100%;
	height: 100%;
}

.chtitle {
	background: #EFEFEF;
	color: whitesmoke;
	display: block;
	width: 71px;
	height: 70px;
	border-right: 1px solid silver;
	/*border-bottom: 1px solid #292929;*/
	position: absolute;
	cursor: pointer;
	box-sizing: content-box;
	color: #868686;
	z-index: 1;
	border-bottom: 1px solid silver;
	box-sizing: border-box;
	background-color: white;
	left: 1px;
}

.grillatv.chunico .chtitle {
	height: auto;
	display: block;
	width: 100%;
	position: relative;
}

.chnumheader {
	background: transparent;
	display: block;
	width: 100%;
	height: 20px;
	text-align: center;
	/* color: #333; */
	font-weight: bold;
	font-size: 12px;
	line-height: 20px;
}

.chtitle.openchtitle .chnumheader {
	display: none;
}

.openchgrilla {
	display: none;
}

.chtitle img {
	background: white;
	padding: 10px;
	border-bottom: none;
	width: auto;
	height: auto;
	max-width: 100%;
	max-height: 50px;
	margin: 0 auto;
	display: block;
}

li.chrow:last-child .chtitle img {
	border-bottom: none;
}

.chname {
	padding: 0 15px;
}

.chnum {
	padding: 0 15px 0 0;
	display: inline-block;
	text-align: right;
	color: #333;
	height: 20px;
	line-height: 20px;
}

.programtime, .programtitle {
	display: block;
	overflow: hidden;
	height: 21px;
	max-height: 21px;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.programtime .programtitle {
	font-weight: bold;
}

.regla {
	pointer-events: none;
	position: absolute;
	width: 1px;
	height: 100vh;
	background: #03A9F4;
	display: block;
	top: 50px;
	left: 0;
}

/* Línea de tiempo */
.grillalineadetiempowrapper {
	display: block;
	height: 50px;
	position: relative;
	z-index: 1;
}

.grillatv.chunico .grillalineadetiempowrapper {
	display: none;
}

.grillalineadetiempo {
	position: absolute;
	top: 0;
}

.grillalineadetiempowrapper .grillalineadetiempo {
	height: 100%;
	-moz-transition: top .3s ease;
	-webkit-transition: top .3s ease;
	-o-transition: top .3s ease;
	transition: top .3s ease;
	overflow: visible;
}

.grillalineadetiempo.fijada {
	box-shadow: 0 0 15px rgba(0, 0, 0, 0.79);
	border-bottom: 1px solid #AFAFAF;
}

.grillalineadetiempo .hourseparator {
	padding: 0;
	height: 50px;
	border-bottom: 1px solid silver;
	border-right: 1px solid silver;
	cursor: default;
	width: 180px;
	max-width: 180px;
	background: #AFAFAF;
	color: white;
}

.grillalineadetiempo .hourseparator.pretime {
    background: #404040;
    width: 220px;
    max-width: 220px;
    border-right: 1px solid #292929;
    border-bottom: 1px solid #292929;
}

.grillalineadetiempo .hourseparator .timestamp {
	display: block;
	width: 100%;
	width: calc(100% - 10px);
	height: 50px;
	vertical-align: middle;
	line-height: 50px;
	padding: 0 0 0 10px;
	overflow: visible;
	white-space: nowrap;
	text-overflow: ellipsis;
	position: relative;
}

.grillalineadetiempo .hourseparator .timestamp .timewrapper {
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
	overflow: hidden;
}

.grillalineadetiempo .hourseparator:last-child .timestamp {
	border-right: none;
}

/* Título de la línea de tiempo */
	.chrow.head {
		height: 50px;
		display: block;
		margin-top: -50px;
		position: absolute;
		width: 220px;
	}

	.chrow.head .chtitle {
		display: inline-block;
		width: 100%;
		height: 100%;
		line-height: 50px;
		position: absolute;
		z-index: 1;
	}

/* END Título de la línea de tiempo */
/* END Línea de tiempo */

/*@media (max-width: 680px) {*/
	ul.listacanales:before {
		content: "";
		display: block;
		position: absolute;
		width: 1px;
		height: calc(100% - 50px);
		background: transparent;
		left: 70px;
		box-shadow: 0 0 6px black;
		top: 50px;
	}

	.grillatv.chunico ul.listacanales:before {
		display: none;
	}

.loveit {
	display: none;
}

.chtitle.openchtitle .loveit {
	background-color: transparent;
	width: 20px;
	height: 20px;
	display: block;
	position: absolute;
	right: 45px;
	cursor: pointer;
	top: 25px;
	background-image: url('/img/love_inactive.png');
	background-size: 100% 100%;
	background-repeat: no-repeat;
}

.chrow.faved .chtitle.openchtitle .loveit {
	background-image: url('/img/love_active.png');
	background-size: 100% 100%;
	background-repeat: no-repeat;
}

.favsbutton.active ~ .searcherblockinner li.chrow {
    display: none;
}

.favsbutton.active ~ .searcherblockinner li.chrow.faved {
    display: table-row;
}

	.openinfoch {
		display: none;
		width: 25px;
		height: 70px;
		background: #878787;
		margin-left: 40px;
		position: absolute;
		cursor: pointer;
		z-index: 1;
	}

	.chtitle.openchtitle .openchgrilla {
		background: white;
		display: block;
		width: 50px;
		position: absolute;
		left: -50px;
		height: 70px;
		border-right: 1px solid silver;
		border-bottom: 1px solid silver;
	}

	.chnum, .chname {
		display: none;
	}

	.chtitle.openchtitle {
		width: auto;
		padding-right: 30px;
		z-index: 2;
		overflow: hidden;
	}

	.chtitle.openchtitle .openinfoch {
		right: 0;
		margin: 0;
		border-right-color: silver;
		display: block;
		background-image: url('/img/botonflechasimple-izquierda-04.svg');
		background-repeat: no-repeat;
		background-position: center;
		background-size: 20px 20px;
	}

	.vermas {
		display: block;
		text-align: left;
		padding: 0 15px;
	}

	.infoch {
		display: none;
	}

	.chtitle.openchtitle .infoch {
		display: inline-block;
		margin-left: -70px;
		min-width: 150px;
		vertical-align: middle;
	}

	.chtitle.openchtitle .infoch {
		margin-left: 0;
		-moz-transition: margin-left .3s ease;
		-webkit-transition: margin-left .3s ease;
		-o-transition: margin-left .3s ease;
		transition: margin-left .3s ease;
	}

/*
.chtitle.openchtitle .infoch:before {
	content: "";
	display: block;
	position: absolute;
	width: 1px;
	height: 100%;
	background: silver;
	left: 70px;
	box-shadow: 0 0 6px black;
	top: 0;
}
*/

.chtitle.openchtitle .infoch:after {
    content: "";
    display: block;
    position: absolute;
    width: 1px;
    height: 100%;
    background: transparent;
    right: 0px;
    box-shadow: 0 0 6px black;
    top: 0;
}

	.chtitle.openchtitle .chnum {
		display: inline-block;
	}

	.chtitle.openchtitle .chnum + span {
		display: inline-block;
		padding: 0 10px;
		border-left: 1px solid silver;
	}

	.chtitle.openchtitle .chname {
		display: block;
		text-align: left;
		text-transform: uppercase;
		font-weight: bold;
	}

	.chtitle.openchtitle .chname {
		width: auto;
		white-space: nowrap;
	}

	.logowrapper {
		background-repeat: no-repeat;
		background-size: contain;
		background-position: center center;
		display: block;
		width: calc(100% - 20px);
		height: 40px;
		margin: 10px auto 0 auto;
	}

	.grillatv.grillatv.chunico .logowrapper {
		display: inline-block;
		width: 60px;
		height: 60px;
		vertical-align: middle;
		margin: 10px;
		background-color: white;
		background-size: calc(100% - 20px);
	}

	.grillatv.grillatv.chunico .chnumheader {
		display: none;
	}

	.chtitle.openchtitle .logowrapper {
		display: inline-block;
		width: 70px;
		height: 100%;
		vertical-align: middle;
		position: relative;
		margin: 0;
		background-color: white;
		background-size: calc(100% - 20px);
		box-shadow: 0 0 18px rgba(0, 0, 0, 0.41);
	}

	.chtitle.openchtitle img {
		width: 72px;
		padding: 10px;
		position: relative;
		z-index: 1;
		border-right: 1px solid silver;
		border-left: 1px solid silver;
		left: -1px;
		display: inline-block;
	}

	.chtitle.openchtitle .logowrapper img {
		display: inline-block;
		width: 100%;
		height: auto;
		max-width: 100%;
		max-height: 100%;
		vertical-align: middle;
		line-height: 70px;
	}


	.grillatv.grillatv.chunico {
		overflow: hidden;
	}

	.grillatv.grillatv.chunico .chtitle {
		background-color: #AFAFAF;
	}

	.grillatv.grillatv.chunico .infoch {
		display: inline-block;
		height: 60px;
		vertical-align: middle;
	}

	.grillatv.grillatv.chunico .chname {
		display: inline-block;
	}

	.grillatv.grillatv.chunico .vermas {
		display: inline-block;
		text-align: left;
		padding: 0;
		float: left;
	}

	.grillatv.grillatv.chunico .vermas span {
		display: none;
	}

	.grillatv.grillatv.chunico .vermas span.chnum {
		display: inline-block;
		color: whitesmoke;
		padding: 0;
	}

	.grillatv.grillatv.chunico .chname, .grillatv.grillatv.chunico .vermas {
		display: block;
		width: auto;
		height: 70px;
		line-height: 70px;
		padding: 0;
		font-size: 35px;
		color: whitesmoke;
	}

	.grillatv.grillatv.chunico .chname {
		font-size: 22px;
		height: 35px;
		line-height: 35px;
	}

	.grillatv.grillatv.chunico .vermas {
		height: 25px;
		line-height: 25px;
		font-size: 16px;
		text-align: left;
	}

	.grillatv.grillatv.chunico .vermas:before {
		content: "Canal ";
	}

	ul.programslist {
		margin-left: 70px;
	}

	.grillalineadetiempo .hourseparator.pretime {
		width: 50px;
		max-width: 50px;
	}

	.chrow.head {
		width: 51px;
	}

	/*
	.chtitle:hover .chnum, .chtitle:hover .chname {
		display: block;
		position: absolute;
		background-color: #404040;
		border: 1px solid #404040;
		color: whitesmoke;
	}

	.chtitle:hover .chnum {
		left: 50px;
		border-right-color: silver;
	}

	.chtitle:hover .chname {
		top: 0;
		left: 94px;
		height: 70px;
		box-sizing: border-box;
		min-width: 180px;
		line-height: 70px;
	}
	*/
/*} */

.grillatv.chunico .botonesslide {
	display: none;
}

.botonesslide {
	background: #333;
	display: block;
	position: absolute;
	height: 50px;
	width: 100%;
	left: 0;
}

.botoneswrapper {
	display: block;
	background: transparent;
	height: 100%;
	width: 100%;
	position: absolute;
	top: 0;
}

.botonslide {
	display: block;
	background: #878787;
	height: 100%;
	width: 72px;
	text-align: center;
	position: absolute;
	z-index: 2;
	top: 0;
	-moz-transition: top .3s ease;
	-webkit-transition: top .3s ease;
	-o-transition: top .3s ease;
	transition: top .3s ease;
}

.botonslide.botonright {
	right: 0;
}

.botonslide.botonleft {
	left: 0;
}

.botonslide .flechaslide {
	background: transparent;
	width: 35px;
	height: 100%;
	display: inline-block;
	background-repeat: no-repeat;
	background-position: center;
	background-size: 30px 30px;
	cursor: pointer;
}

.botonleft .flechaslide.simple {
	background-image: url('/img/botonflechasimple-izquierda-04.svg');
	background-position: 10px center;
}

.botonleft .flechaslide.doble {
	background-image: url('/img/botonflecha-izquierda-04.svg');
	background-position: -3px center;
}

.botonright .flechaslide.simple {
	background-image: url('/img/botonflechasimple-derecha-04.svg');
	background-position: -3px center;
}

.botonright .flechaslide.doble {
	background-image: url('/img/botonflecha-derecha-04.svg');
	background-position: 10px center;
}

@media (max-width: 699px), (orientation: portrait) {
	.botonleft .flechaslide.simple {
		background-image: url('/img/botonflechasimple-izquierda-04.svg');
		background-position: 10px center;
	}
	
	.botonleft .flechaslide.doble {
		background-image: url('/img/botonflecha-izquierda-04.svg');
		background-position: -3px center;
	}
	
	.botonright .flechaslide.simple {
		background-image: url('/img/botonflechasimple-derecha-04.svg');
		background-position: -3px center;
	}
	
	.botonright .flechaslide.doble {
		background-image: url('/img/botonflecha-derecha-04.svg');
		background-position: 10px center;
	}
}


@media (max-width: 840px) {
	.flechaslide.doble {
		display: none;
	}

	.botonslide .flechaslide, .botonright .flechaslide.simple, .botonleft .flechaslide.simple {
		width: 100%;
		background-position: center;
	}
}

.searcherblockwrapper {
	position: relative;
}

.searcherblockwrapper .filterpremiumwrapper, .searcherblockwrapper .filterhdwrapper {
	position: absolute;
	right: 0;
}

.searcherblockwrapper .extrafilters .filterpremiumwrapper, .searcherblockwrapper .extrafilters .filterhdwrapper {
	position: relative;
	right: initial;
	float: none;
	padding: 0;
	vertical-align: middle;
	display: inline-block;
	margin: 14px 0;
	height: 27px;
}

.searcherblockwrapper .extrafilters .filterhdwrapper + .filterpremiumwrapper {
	border-left: 1px solid silver;
	margin-left: 20px;
	padding-left: 20px;
}

.searcherblockwrapper .extrafilters .filterhdwrapper.nofilter + .filterpremiumwrapper {
	border: none;
	margin-left: inherit;
	padding-left: inherit;
}

.extrafilters {
	float: right;
	position: absolute;
	right: 0;
	top: 8px;
}

.withfavs .extrafilters .favsbutton {
	margin: 0;
	padding: 0 40px 0 20px;
	width: auto;
	height: 25px;
	box-sizing: content-box;
	background-origin: border-box;
	background-position: center right;
}

.withfavs .extrafilters .filterpremiumwrapper + .favsbutton, .withfavs .extrafilters .filterhdwrapper + .favsbutton {
	margin: 0 0 0 20px;
	padding: 0 40px 0 20px;
	border-left: 1px solid silver;
}

.withfavs .extrafilters .favsbutton:first-child {
	margin-top: 16px;
}

.searcherblockwrapper select + select {
	/*margin-left: 5px;*/
}

.searcherblockwrapper .checkitems {
	width: auto;
}


@media (max-width: 920px) {
	.extrafilters {
		position: relative;
		float: none;
		top: 0;
		right: initial;
		margin: 0 0 10px 0;
	}
}

@media (max-width: 420px) {
	.searcherblockwrapper .filterpremiumwrapper, .searcherblockwrapper .filterhdwrapper {
		position: relative;
		left: 0;
		float: none;
		display: block;
		width: 140px;
		margin: 0 0 15px 0;
	}

	.searcherblockwrapper .extrafilters .filterpremiumwrapper, .searcherblockwrapper .extrafilters .filterhdwrapper {
		position: relative;
		right: initial;
		float: none;
		padding: 0;
		display: inline-block;
		vertical-align: top;
		margin: 14px 0;
	}

	.filterpremiumwrapper .checkitems, .filterhdwrapper .checkitems {
		text-align: left;
	}

	.searcherblockwrapper .checkitems {
		width: auto;
		display: block;
	}
}

@media (max-width: 600px) {
	.searcherblockwrapper .extrafilters .filterhdwrapper,
	.searcherblockwrapper .extrafilters .filterpremiumwrapper,
	.searcherblockwrapper .extrafilters .filterhdwrapper + .filterpremiumwrapper,
	.withfavs .extrafilters .favsbutton,
	.withfavs .extrafilters .filterpremiumwrapper + .favsbutton {
		border: none;
		display: inline-block;
		width: 100%;
		padding-left: 0;
		margin: 5px 0;
	}

	.withfavs .extrafilters .favsbutton,
	.withfavs .extrafilters .filterpremiumwrapper + .favsbutton {
		margin: 5px 0;
		width: 100%;
		background-position-x: calc(100% - 1px);
		padding: 0;
	}

	.filterpremiumwrapper .checkitems, .filterhdwrapper .checkitems {
		text-align: left;
	}

	.withfavs .favsbutton span {
		display: inline-block;
	}
}

.searcherblockwrapper select + .searchbutton {
	margin-left: 5px;
}

.searchbutton, .searchbutton:hover {
	padding: 0 1em;
	margin: 1em auto;
	display: none;
	cursor: pointer;
	border: 1px solid silver;
	border-radius: 3px;
	background: white;
	vertical-align: bottom;
	height: 36px;
	line-height: 36px;
	color: inherit;
	text-decoration: none;
	margin-right: 5px;
}

.favsbutton {
	display: none;
	width: 30px;
	height: 40px;
	vertical-align: middle;
	cursor: pointer;
	background-color: transparent;
	background-image: url("/img/hrtbtninactive.png");
	background-repeat: no-repeat;
	background-size: contain;
	background-position: center;
}

.favsbutton span {
	color: #178BE6;
	font-weight: bold;
	line-height: 27px;
	display: inline-block;
}

.withfavs .favsbutton {
	display: inline-block;
}

.favsbutton:active, .favsbutton.active {
	background-image: url("/img/hrtbtnactive.png");
	background-repeat: no-repeat;
	background-size: contain;
	background-position: center;
}

select.selectday, select.selectcategory {
	margin-right: 5px;
}

.searchbutton, .searchbutton:hover, .searchbutton:active, .searchbutton:focus {
	outline: none;
	text-decoration: none;
	color: inherit;
}

.searchbutton:active {
	box-shadow: 0 0 4px rgba(0, 0, 0, 0.22) inset;
}

.searcherblockwrapper.chunico .searchbutton {
	display: inline-block;
}

.searcherblockwrapper.chunico .selectcategory, .searcherblockwrapper.chunico .filterpremiumwrapper, .searcherblockwrapper.chunico .filterhdwrapper {
	display: none;
}

/* END Grilla */
.buscador .xxxchannelbrickwrap, .buscadorficha .xxxchannelbrickwrap, {
	cursor: pointer;
}

.buscador .xxxchannelbrickwrap:active .xxxchannelsbrick, .buscadorficha .xxxchannelbrickwrap:active .xxxchannelsbrick, {
	box-shadow: 0 0 10px rgba(0, 0, 0, 0.33) inset;
}

/* Ficha */
	.ficha {
		display: block;
		width: 500px;
		max-width: 100%;
		font-size: 16px;
	}

	.ficha .eventdetails {
		display: block;
	}

	.eventdetailgroup {
		display: table-row;
	}

	.ficha .pic {
		display: table-cell;
		margin: 0 0 0 0;
		padding: 0 0 0 0;
		width: 222px;
		max-width: 100%;
		height: 150px;
	}

	.tecdetails {
		display: table-cell;
		vertical-align: top;
	}

	.pic + .tecdetails {
		padding-left: 10px;
	}

	.ficha h3.title {
		display: block;
		margin: 0 0 4px 0;
		padding: 0;
	}

	p.info {
		margin: 0;
		padding: 0;
	}

	p.info + p.info {
		margin-top: 12px;
	}

	.eventdetailgroup + p.info:first-of-type {
		margin-top: 12px;
	}

	.info > span {
		display: inline;
		line-height: 1;
	}

	.info.basics span span {
		white-space: nowrap;
	}

	.info > span + span, .info.ocurrencias > span span + span {
		border-left: 1px solid silver;
		padding-left: 5px;
		margin-left: 5px;
	}

	.info > span.subtitle + span {
		padding: 0px;
		margin: 0px;
		border: none;
	}

	.info.ocurrencias > span span, .info.ocurrencias > span span + span {
		margin-left: 0;
		margin-right: 5px;
	}

	.info.ocurrencias {
		margin: 10px 0;
	}

	.info.ocurrencias > span {
		border: 0;
		display: block;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		line-height: 1.4;
		font-weight: bold;
		text-transform: uppercase;
	}

	.info.ocurrencias > span span {
		display: inline;
		display: inline-block;
		font-weight: normal;
		text-transform: none;
	}

	.info.ocurrencias > span span.ocurrenciacanal {
		font-weight: bold;
	}

	p.info.sinopsis, .info.ocurrencias, p.info + .info.ocurrencias {
		display: block;
		margin: 10px 0 0 0;
		padding: 10px 0 0 0;
		/*border-top: 1px solid silver;*/
		overflow: hidden;
	}

	.filtrable {
		cursor: pointer;
	}
	
	.filtrable:hover {
		text-decoration: underline;
	}

	p.info .filtrable, .subtitle + span {
		color: #005A96;
	}

	p.info.basics .filtrable {
		color: inherit;
	}

	.partidoequipos {
		text-align: center;
		padding: 10px 0 0 0;
		margin: 10px 0 0 0;
	}

	.topinfo {
		display: table;
		position: relative;
		min-height: 150px;
	}

	.topinfobottomwrapper {
		display: table-row;
		display: inline-block;
		width: 100%;
	}

	.topinfobottom {
		display: table-cell;
		position: relative;
		vertical-align: bottom;
	}

	/* Ocurrencias */
	.ocurrenciacanalbloque {
		display: block;
		width: calc(100% + 30px);
	}

	.ocurrenciacanalbloque + .ocurrenciacanalbloque {
		margin-top: 12px;
	}

	.canaltitle {
		font-weight: bold;
		font-size: 18px;
		margin-bottom: 5px;
	}

	.ocurrenciadiabloque {
		display: inline-block;
		vertical-align: top;
		width: 14.28%;
		max-width: 14.28%;
		margin-bottom: 20px;
	}

	.emisionhorario {
		display: inline-block;
		width: 100%;
	}

	.daytitle {
		display: inline-block;
		width: 100%;
		font-weight: bold;
		font-size: 18px;
	}

	@media (max-width: 550px) {
		.ocurrenciacanalbloque {
			width: 100%;
		}
	}

	@media (max-width: 380px) {
		.ocurrenciadiabloque {
			font-size: 13px;
		}

		.daytitle {
			font-size: 14px;
		}
	}
	/* END Ocurrencias */
	
	/* Rating */
	.rating.basis {
		display: inline-block;
		background-image: url('/img/estrellasmagentas-01-01.svg');
		background-position: 0 13px;
		background-size: 70px 26px;
		width: 70px;
		height: 13px;
		vertical-align: top;
	}

	.rating.basis + .rating {
		display: inline-block;
		background: url('/img/estrellasmagentas-01-01.svg');
		background-position: 0 0;
		background-size: 70px 26px;
		width: 100%;
		height: 100%;
		vertical-align: bottom;
		margin: 0;
		padding-right: 0;
		box-sizing: content-box;
		position: absolute;
		top: 0;
		left: 0;
	}

	.rating.basis + .rating.puntos1 {
		width: 7px;
	}

	.rating.basis + .rating.puntos2 {
		width: 14px;
	}

	.rating.basis + .rating.puntos3 {
		width: 21px;
	}

	.rating.basis + .rating.puntos4 {
		width: 28px;
	}

	.rating.basis + .rating.puntos5 {
		width: 35px;
	}

	.rating.basis + .rating.puntos6 {
		width: 42px;
	}

	.rating.basis + .rating.puntos7 {
		width: 49px;
	}

	.rating.basis + .rating.puntos8 {
		width: 56px;
	}

	.rating.basis + .rating.puntos9 {
		width: 63px;
	}

	.rating.basis + .rating.puntos10 {
		width: 70px;
	}
	/* END Rating */

	.info.originaltitle > span.h3 {
		display: block;
		margin: 0;
		padding: 0;
		padding-bottom: 0;
	}

	p.info span.subtitle {
		border: none;
		padding: 0;
		margin: 0;
		width: 100%;
		display: inline-block;
	}

	/* Mobile */
	@media (max-width: 450px) {
		.ficha .pic {
			display: block;
			float: right;
			/*width: 35%;*/
			width: 50%;
			height: auto;

			float: none;
			width: 100%;
		}

		.topinfo {
			min-height: 0;
		}
		
		/*
		.tecdetails {
			display: table-cell;
			vertical-align: top;
			display: block;
		}

		.eventdetailgroup {
			display: table;
			width: 100%;
		}

		.pic + .tecdetails {
			padding-left: 0;
		}
		*/
	}
	/* END Mobile */
/* END Ficha */

/* Popover guía */
#popoverguia {
	position: absolute;
	background: rgba(0, 0, 0, 0.79);
	width: 240px;
	max-width: 90%;
	display: block;
	padding: 20px;
	margin-top: 14px;
	border-radius: 10px;
}

#popoverguia:before {
	content: "";
	display: block;
	position: absolute;
	width: 0;
	height: 0;
	background: transparent;
	top: -10px;
	left: 80px;
	border-left: 20px solid rgba(0, 0, 0, 0.79);
	border-bottom: 20px solid transparent;
	transform: rotate(45deg);
}
/* END Popover guía */

/* Colores Buscador */
.headertop, .stickedmenu .headertop, #menuwrapper > ul > li.tvsearchwrapper.srchactive:hover, #menuwrapper > ul > li.tvsearchwrapper.srchactive:hover > a, .eventlabel.futuro {
	background: #2196F3;
	background-color: #2196F3;
}

.srchactive .closesearcher {
	border-color: #2196F3;
}

h1, .h1, h2, .h2, h3, .h3, h4, .h4, .info.ocurrencias > span.title, .checkitems span, .checkitems label, .inputtitle {
	color: #178BE6;
}

.check label {
	/*border: 2px solid #0089E7;*/
	border: none;
	background: #eaeaea;
}

.check input:checked + label {
	background-image: url('/img/tildemagenta-01.svg');
}

.eventlabel.ahora {
	background: #03B303;
}

.packpicklist a.selected:after, .eventlabel.viejo, .progressbar {
	background: #FF2793;
}

select.catselect, .catselectwrapper > select {
	padding-right: 30px;
	background-image: url('../img/flecha-01.svg');
	background-size: 10px 10px;
	background-repeat: no-repeat;
	background-position: calc(100% - 10px) center;
}

@media (min-width: 700px) and (orientation: landscape) {
	#menuwrapper > ul > li > a, .stickedmenu #menuwrapper > ul > li > a, #menuwrapper > ul > li:hover, #menuwrapper > ul > li.hover, #menuwrapper > ul > li.tvsearchwrapper:hover > a {
		background: #2196F3;
		background-color: #2196F3;
	}

	#menuwrapper > ul > li:hover, #menuwrapper > ul > li.hover, #menuwrapper > ul > li:hover > a {
		background: #1381D8;
	}
}

@media (max-width: 699px), (orientation: portrait) {
	li.tvsearchwrapper.srchactive > a, #menuwrapper > ul > li.tvsearchwrapper.srchactive > a {
		background: #2196F3;
	}
}
/* END Colores Buscador */


/* Nueva estructura */
		.check label {
			background: rgba(0,0,0,0.085);
		}

		/* Filtro favoritos en grilla */
		.withfavs .favsbutton {
			display: inline-block;
		}

		.searcherblockwrapper.chunico .favsbutton {
			display: none;
		}

		.chtitle.openchtitle .loveit {
			display: block;
		}

		.favsbutton {
			height: 30px;
			background-image: url("/img/corazon_favs_off.svg");
			background-color: transparent;
		}

		.favsbutton:active, .favsbutton.active {
			background-image: url("/img/corazon_favs_on.svg");
			background-color: transparent;
		}

		.chtitle.openchtitle .loveit {
			background-image: url("/img/corazon_favs_off.svg");
			background-color: transparent;
		}

		.chrow.faved .chtitle.openchtitle .loveit {
			background-image: url("/img/corazon_favs_on.svg");
			background-color: transparent;
		}

		.favsbutton.active ~ .searcherblockinner li.chrow, .favsbutton.active ~ .searcherblockinner .grillatv.filtered li.chrow,
		.withfavs.activefavs .searcherblockinner li.chrow, .withfavs.activefavs .searcherblockinner .grillatv.filtered li.chrow {
			display: none;
		}
		
		.favsbutton.active ~ .searcherblockinner .grillatv:not(.filtered) li.chrow.faved, .favsbutton.active ~ .searcherblockinner .grillatv.filtered li.chrow.pasa.faved, 
		.withfavs.activefavs .searcherblockinner .grillatv:not(.filtered) li.chrow.faved, .withfavs.activefavs .searcherblockinner .grillatv.filtered li.chrow.pasa.faved {
			display: inherit;
		}

		.faving .chtitle .logowrapper, .defaving .chtitle .logowrapper, .faving .chtitle .chnumheader, .defaving .chtitle .chnumheader {
			opacity: .2;
		}

		li.chrow.faved.faving:before {
			content: "";
			display: block;
			position: absolute;
			width: 0;
			height: 0;
			z-index: 3;
			background-color: transparent;
			background-image: url("/img/corazon-faving.svg");
			background-size: 100%;
			background-repeat: no-repeat;
			background-position: center;
			margin: 0 0 0 0;
			animation-name: faving;
			animation-duration: .8s;
		}

		@keyframes faving {
			0% {
				width: 0;
				height: 0;
				margin: 35px 0 0 35px;
				transform: rotate(-40deg);
			}
			50% {
				/*width: 120px;*/
				width: 70px;
				/*height: 120px;*/
				height: 70px;
				/*margin: -25px 0 0 -25px;*/
				margin: 5px 0 0 -5px;
				/*transform: rotate(40deg);*/
				transform: rotate(40deg);

			}
			100% {
				width: 0;
				height: 0;
				margin: 35px 0 0 35px;
				transform: rotate(0deg);
			}
		}

		li.chrow.defaving:before {
			content: "";
			display: block;
			position: absolute;
			width: 0;
			height: 0;
			z-index: 3;
			background-color: transparent;
			background-image: url("/img/corazongrieta-left-off.svg");
			background-size: 100% 100%;
			background-repeat: no-repeat;
			background-position: top left;
			margin: 0 0 0 0;
			animation-name: defavingLeft;
			animation-duration: .8s;
		}

		li.chrow.defaving:after {
			content: "";
			display: block;
			position: absolute;
			width: 0;
			height: 0;
			z-index: 3;
			background-color: transparent;
			background-image: url("/img/corazongrieta-right-off.svg");
			background-size: 100% 100%;
			background-repeat: no-repeat;
			background-position: top right;
			margin: 0 0 0 0;
			animation-name: defavingRight;
			animation-duration: .8s;
		}

		@keyframes defavingLeft {
			0% { /* Arranca en tamaño cero, desde el centro */
				width: 0;
				height: 0;
				margin: 35px 0 0 35px;
				transform: rotate(0deg) translate(0, 0);
				background-image: url("/img/corazongrieta-left-on.svg");
			}
			10% { /* Alcanza el máximo tamaño */
				width: 20px;
				height: 35px;
				margin: 15px 0 0 19px;
				transform: rotate(0deg) translate(0, 0);
			}
			25% {
				width: 20px;
				height: 35px;
				margin: 15px 0 0 19px;
				transform: rotate(0deg) translate(0, 0);
			}
			26% { /* crack */
				width: 20px;
				height: 35px;
				margin: 15px 0 0 17px;
				transform: rotate(-6deg) translate(0, 0);
			}
			50% {
				background-image: url("/img/corazongrieta-left-on.svg");
			}
			60% { /* Desde acá empieza a perder el color */
				width: 20px;
				height: 35px;
				margin: 15px 0 0 17px;
				transform: rotate(-6deg) translate(0, 0);
			}
			70% { /* Alcanza el suelo y está completamente en color gris */
				width: 20px;
				height: 35px;
				margin: 35px 0 0 17px;
				background-image: url("/img/corazongrieta-left-off.svg");
				opacity: 1;
			}
			100% { /* Cae de costado en el suelo y se desvanese */
				width: 20px;
				height: 35px;
				margin: 35px 0 0 14px;
				transform: rotate(-59deg) translate(-11px, 0px);
				opacity: 0;
			}
		}

		@keyframes defavingRight {
			0% { /* Arranca en tamaño cero, desde el centro */
				width: 0;
				height: 0;
				margin: -35px 0 0 35px;
				transform: rotate(0deg) translate(0, 0);
				background-image: url("/img/corazongrieta-right-on.svg");
			}
			10% { /* Alcanza el máximo tamaño */
				width: 20px;
				height: 35px;
				margin: -55px 0 0 32px;
				transform: rotate(0deg) translate(0, 0);
			}
			25% {
				width: 20px;
				height: 35px;
				margin: -55px 0 0 32px;
				transform: rotate(0deg) translate(0, 0);
			}
			26% { /* crack */
				width: 20px;
				height: 35px;
				margin: -55px 0 0 34px;
				transform: rotate(6deg) translate(0, 0);
			}
			50% {
				background-image: url("/img/corazongrieta-right-on.svg");
			}
			60% { /* Desde acá empieza a perder el color */
				width: 20px;
				height: 35px;
				margin: -55px 0 0 34px;
				transform: rotate(6deg) translate(0, 0);
			}
			70% { /* Alcanza el suelo y está completamente en color gris */
				width: 20px;
				height: 35px;
				margin: -35px 0 0 34px;
				background-image: url("/img/corazongrieta-right-off.svg");
				opacity: 1;
			}
			100% { /* Cae de costado en el suelo y se desvanese */
				width: 20px;
				height: 35px;
				margin: -35px 0 0 43px;
				transform: rotate(59deg) translate(9px, 4px);
				opacity: 0;
			}
		}
		/* END Filtro favoritos en grilla */

		#searchtvform .submitwrapper .xxxbuttonget, #searchtvform .submitwrapper .xxxbuttonget:hover, #searchtvform .submitwrapper .xxxbuttonget:focus {
			background-color: #ff2793;
		}

		.inputsgroup, .inputsgroup + .inputsgroup {
			border: none;
		}

		#searchtvform .submitwrapper .xxxbuttonget:active {
			background-color: #f50a79;
		}

		.avanzadas {
			display: none;
			border: none;
		}

		.avanzadas.opened {
			display: block;
			position: relative;
			padding-top: 30px;
		}

		.avanzadas.opened:before {
			content: "";
			width: calc(100% - 24px);
			height: 1px;
			background: rgba(192, 192, 192, 0.55);
			position: absolute;
			display: block;
			top: 0;
			left: 12px;
		}

		.avanzadas + .openavanzadas {
			width: 28px;
			height: 28px;
			background: rgba(0,0,0,0.085);
			display: block;
			position: relative;
			margin: 5px auto;
			cursor: pointer;
			border-radius: 4px;
		}

		.avanzadas + .openavanzadas:active {
			background: #dcdcdc;
		}

		.avanzadas + .openavanzadas:after {
			content: "";
			width: 8px;
			height: 8px;
			display: block;
			position: absolute;
			top: calc(50% - 4px);
			left: calc(50% - 4px);
			background: transparent;
			border-right: 2px solid #2196f3;
			border-bottom: 2px solid #2196f3;
			transform: rotate(45deg) translate(-1px,-1px);
			cursor: pointer;
			margin: 0;
			box-sizing: border-box;
		}

		.avanzadas.opened + .openavanzadas:after {
			border-right: none;
			border-bottom: none;
			border-left: 2px solid #2196f3;
			border-top: 2px solid #2196f3;
			transform: rotate(45deg) translate(1px,1px);
			background: transparent;
		}

		.submitwrapper .inputwrapper {
			margin: 10px auto;
		}

		@media (max-width: 699px), (orientation: portrait) {
			.avanzadas + .openavanzadas {
				margin-top: 18px;
				margin-bottom: 28px;
			}

			/* Input de búsqueda en mobile */
			li.tvsearchwrapper.srchactive, #menuwrapper > ul > li.tvsearchwrapper.srchactive {
				height: 46px;
			}

			li.tvsearchwrapper.srchactive > a, #menuwrapper > ul > li.tvsearchwrapper.srchactive > a {
				line-height: 46px;
				padding: 0 60px 0 24px;
			}

			li.tvsearchwrapper.srchactive > a > .tvsearch, #menuwrapper > ul > li.tvsearchwrapper.srchactive > a > .tvsearch {
				height: 33px;
				margin: 0 0 0 0;
			}

			.closesearcher {
				/*border-width: 20px;*/
				/*transition: border-width .5s ease;*/
				height: 46px;
				width: 46px;
				right: 8px;
			}

			.tvsearchwrapper.srchactive #closemodal, .tvsearchwrapper.srchactive .closemodal {
				top: 8px;
			}
			/* END Input de búsqueda en mobile */

			.buscadoravanzado.srchactive {
				top: 46px;
				max-height: calc(100vh - 46px);
			}

			.submitwrapper {
				top: calc(100vh - 144px);
			}

			.inputsgroupswrapper {
				height: calc(100vh - 132px);
				top: -12px;
			}

			.avanzadas.opened {
				margin-top: 20px;
			}

			#searchtvform .inputsgroupswrapper {
				padding: 0;
			}

			.submitwrapper .inputwrapper {
				padding-right: 24px;
				padding-left: 24px;
			}
		}

		@media (min-width: 700px) and (orientation: landscape) {
			body.buscador.activesearch {
				position: static;
				overflow-y: scroll;
				width: auto;
			}
	
			.submitwrapper {
				display: none;
			}

			.buscadoravanzado.srchactive, .buscadoravanzado.srchactive .srchoptions, .srchoptions .form, form#searchtvform, #searchtvform .inputsgroupswrapper {
				height: auto;
			}

			.buscadoravanzado.srchactive .srchoptions, .srchoptions .form, form#searchtvform, #searchtvform .inputsgroupswrapper {
				position: relative;
			}

			.buscadoravanzado.srchactive {
				max-width: 100%;
				position: fixed;
				left: 0;
				box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
				z-index: 10;
				max-height: calc(100% - 90px);
				overflow-y: auto;
			}

			.srchoptions .inputwrapper {
				width: 33.33333%;
				/*margin: 0;*/
				padding-top: 0;
				padding-bottom: 0;
			}

			.srchoptions .inputtitle {
				margin-bottom: 0;
			}

			.srchoptions #searchtvform .inputtext, .srchoptions #searchtvform .catselectwrapper select {
				margin-top: 10px;
				margin-bottom: 10px;
			}

			.buscadoravanzado.srchactive .blockcontainer {
				padding-right: 0;
				padding-left: 0;
			}

			.tvsearchwrapper.srchactive .closesearcher, .tvsearchwrapper.srchactive .closemodal {
				display: none;
			}

			.srchactive .opensearchbutton, .activatingsearch .opensearchbutton {
				display: inline-block;
				right: 0;
				left: initial;
				cursor: pointer;
				top: 2px;
			}

			.srchactive .opensearchbutton:active, .activatingsearch .opensearchbutton:active {
				background-size: 25px 25px;
			}

			/* Grupo de checks justificado */
			.inputsgroup.justifiedgroup {
				text-align: justify;
				-ms-text-justify: distribute-all-lines;
				text-justify: distribute-all-lines;
				padding: 0 12px;
			}

			.inputsgroup.justifiedgroup:after {
				content: "";
				width: 100%;
				display: inline-block;
				font-size: 0;
				line-height: 0
			}

			.srchoptions .inputsgroup.justifiedgroup .inputwrapper {
				vertical-align: top;
				display: inline-block;
				*display: inline;
				zoom: 1;
				width: auto;
				/*max-width: 25%;*/
				padding: 0;
			}

			.srchoptions .inputsgroup.justifiedgroup .inputwrapper .checkitems {
				display: inline-block;
				width: auto;
			}
			/* END Grupo de checks justificado */

			/* Slides agrupados */
			.slidersgroup .searcherblockwrapper.active.toleft {
				transition: none;
			}
			
			.slidersgroup .searcherblockwrapper.active.toright {
				transition: none;
			}
			
			.slidersgroup .searcherblockwrapper.toactivate.fromleft.activating, .slidersgroup .searcherblockwrapper.toactivate.fromright.activating {
				transition: none;
			}
			/* END Slides agrupados */
		}

/* END Nueva estructura */

/* stylebuscador */
/* Estilos especiales */

body.buscador {
	/* buscador dark */
	/*background: #121212;*/
	/* descartado color: silver;*/
	/* END buscador dark */
}

/* Logo en la barra superior */
body.buscador #headdoslogoinline, body.buscador .stickedmenu #headdoslogoinline {
	position: relative;
	top: 0;
	/*background: url('../img/logo_texto-19092018.svg') no-repeat center center;*/
	background: url('../img/logo_texto-19092018.svg') no-repeat center center;
	background-size: contain;
}

#headdoslogointop {
	/*background: url('../img/logo_texto-19092018.svg') no-repeat center center;*/
	background: url('../img/logo_texto-19092018.svg') no-repeat center center;
	background-size: contain;
}
/* Logo en la barra superior */

.buscador > .section:nth-child(odd) {
	/* buscador dark */
	/*background: rgba(255, 255, 255, 0.05);*/
	/* END buscador dark */
	background: rgb(242, 242, 242);
}

/* END stylebuscador */
/* END Social Links */