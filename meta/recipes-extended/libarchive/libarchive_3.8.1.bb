SUMMARY = "Support for reading various archive formats"
DESCRIPTION = "C library and command-line tools for reading and writing tar, cpio, zip, ISO, and other archive formats"
HOMEPAGE = "http://www.libarchive.org/"
SECTION = "devel"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://COPYING;md5=7ce08437ff7f5e24d72e666313ae4084"

DEPENDS = "e2fsprogs-native"

PACKAGECONFIG ?= "zlib bz2 xz zstd ${@bb.utils.filter('DISTRO_FEATURES', 'acl xattr', d)}"

DEPENDS_BZIP2 = "bzip2-replacement-native"
DEPENDS_BZIP2:class-target = "bzip2"

PACKAGECONFIG[acl] = "--enable-acl,--disable-acl,acl,"
PACKAGECONFIG[xattr] = "--enable-xattr,--disable-xattr,attr,"
PACKAGECONFIG[zlib] = "--with-zlib,--without-zlib,zlib,"
PACKAGECONFIG[bz2] = "--with-bz2lib,--without-bz2lib,${DEPENDS_BZIP2},"
PACKAGECONFIG[xz] = "--with-lzma,--without-lzma,xz,"
PACKAGECONFIG[openssl] = "--with-openssl,--without-openssl,openssl,"
PACKAGECONFIG[libb2] = "--with-libb2,--without-libb2,libb2,"
PACKAGECONFIG[libxml2] = "--with-xml2,--without-xml2,libxml2,"
PACKAGECONFIG[expat] = "--with-expat,--without-expat,expat,"
PACKAGECONFIG[lzo] = "--with-lzo2,--without-lzo2,lzo,"
PACKAGECONFIG[nettle] = "--with-nettle,--without-nettle,nettle,"
PACKAGECONFIG[lz4] = "--with-lz4,--without-lz4,lz4,"
PACKAGECONFIG[mbedtls] = "--with-mbedtls,--without-mbedtls,mbedtls,"
PACKAGECONFIG[zstd] = "--with-zstd,--without-zstd,zstd,"

EXTRA_OECONF += "--enable-largefile --without-iconv"

SRC_URI = "https://libarchive.org/downloads/libarchive-${PV}.tar.gz"
UPSTREAM_CHECK_URI = "https://www.libarchive.org/"

SRC_URI[sha256sum] = "bde832a5e3344dc723cfe9cc37f8e54bde04565bfe6f136bc1bd31ab352e9fab"

inherit autotools update-alternatives pkgconfig

CPPFLAGS += "-I${WORKDIR}/extra-includes"

do_configure[cleandirs] += "${WORKDIR}/extra-includes"
do_configure:prepend() {
	# We just need the headers for some type constants, so no need to
	# build all of e2fsprogs for the target
	cp -R ${STAGING_INCDIR_NATIVE}/ext2fs ${WORKDIR}/extra-includes/
}

ALTERNATIVE_PRIORITY = "80"

PACKAGES =+ "bsdtar"
FILES:bsdtar = "${bindir}/bsdtar"

ALTERNATIVE:bsdtar = "tar"
ALTERNATIVE_LINK_NAME[tar] = "${base_bindir}/tar"
ALTERNATIVE_TARGET[tar] = "${bindir}/bsdtar"

PACKAGES =+ "bsdcpio"
FILES:bsdcpio = "${bindir}/bsdcpio"

ALTERNATIVE:bsdcpio = "cpio"
ALTERNATIVE_LINK_NAME[cpio] = "${base_bindir}/cpio"
ALTERNATIVE_TARGET[cpio] = "${bindir}/bsdcpio"

BBCLASSEXTEND = "native nativesdk"
