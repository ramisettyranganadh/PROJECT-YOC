SUMMARY = "Tool for managing software RAID under Linux"
HOMEPAGE = "http://www.kernel.org/pub/linux/utils/raid/mdadm/"
DESCRIPTION = "mdadm is a Linux utility used to manage and monitor software RAID devices."

# Some files are GPL-2.0-only while others are GPL-2.0-or-later.
LICENSE = "GPL-2.0-only & GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
                    file://mdmon.c;beginline=4;endline=18;md5=af7d8444d9c4d3e5c7caac0d9d34039d \
                    file://mdadm.h;beglinlne=4;endline=22;md5=462bc9936ac0d3da110191a3f9994161"

SRC_URI = "git://git.kernel.org/pub/scm/utils/mdadm/mdadm.git;protocol=https;branch=main;tag=mdadm-${PV} \
           file://run-ptest \
           file://0001-Fix-the-path-of-corosync-and-dlm-header-files-check.patch \
           file://mdadm.init \
           file://0001-Makefile-install-mdcheck.patch \
           file://0001-restripe.c-Use-_FILE_OFFSET_BITS-to-enable-largefile.patch \
           file://0002-Create.c-include-linux-falloc.h-for-FALLOC_FL_ZERO_R.patch \
           file://xmalloc.patch \
           "

SRCREV = "8e56efac9afd7080bb42bae4b77cdad5f345633a"

inherit ptest systemd

DEPENDS = "udev"

SYSTEMD_SERVICE:${PN} = "mdmonitor.service"
SYSTEMD_AUTO_ENABLE = "disable"

# PPC64 and MIPS64 uses long long for u64 in the kernel, but powerpc's asm/types.h
# prevents 64-bit userland from seeing this definition, instead defaulting
# to u64 == long in userspace. Define __SANE_USERSPACE_TYPES__ to get
# int-ll64.h included
CFLAGS:append:powerpc64 = ' -D__SANE_USERSPACE_TYPES__'
CFLAGS:append:mipsarchn64 = ' -D__SANE_USERSPACE_TYPES__'
CFLAGS:append:mipsarchn32 = ' -D__SANE_USERSPACE_TYPES__'

EXTRA_OEMAKE = 'CHECK_RUN_DIR=0 CWFLAGS="" CXFLAGS="${CFLAGS}" SYSTEMD_DIR=${systemd_system_unitdir} \
                BINDIR="${base_sbindir}" UDEVDIR="${nonarch_base_libdir}/udev" LDFLAGS="${LDFLAGS}" \
                SYSROOT="${STAGING_DIR_TARGET}" STRIP='

DEBUG_OPTIMIZATION:append = " -Wno-error"

do_install() {
        oe_runmake 'DESTDIR=${D}' install install-systemd
        install -d ${D}/${sysconfdir}/
        install -m 644 ${S}/documentation/mdadm.conf-example ${D}${sysconfdir}/mdadm.conf
        install -d ${D}/${sysconfdir}/init.d
        install -m 755 ${UNPACKDIR}/mdadm.init ${D}${sysconfdir}/init.d/mdmonitor
}

do_compile_ptest() {
	oe_runmake test
}

do_install_ptest() {
	cp -R --no-dereference --preserve=mode,links -v ${S}/tests ${D}${PTEST_PATH}/tests
	cp ${S}/test ${D}${PTEST_PATH}
	sed -e 's!sleep 0.*!sleep 1!g; s!/var/tmp!/mdadm-testing-dir!g' -i ${D}${PTEST_PATH}/test
        sed -i -e '/echo -ne "$_script... "/d' \
               -e 's/echo "succeeded"/echo -e "PASS: $_script"/g' \
               -e '/save_log fail/N; /_fail=1/i\\t\t\techo -ne "FAIL: $_script"' \
               -e '/die "dmesg prints errors when testing $_basename!"/i\\t\t\t\techo -ne "FAIL: $_script" &&' \
               ${D}${PTEST_PATH}/test

        chmod +x ${D}${PTEST_PATH}/test

	ln -s ${base_sbindir}/mdadm ${D}${PTEST_PATH}/mdadm
	for prg in test_stripe swap_super raid6check
	do
		install -D -m 755 $prg ${D}${PTEST_PATH}/
	done

	# Disable tests causing intermittent autobuilder failures
	echo "intermittent failure on autobuilder" > ${D}${PTEST_PATH}/tests/19raid6check.broken
	echo "intermittent failure on autobuilder" > ${D}${PTEST_PATH}/tests/20raid5journal.broken
	echo "intermittent failure on autobuilder" > ${D}${PTEST_PATH}/tests/21raid5cache.broken
	echo "intermittent failure on autobuilder" > ${D}${PTEST_PATH}/tests/10ddf-fail-spare.broken
	echo "intermittent failure on autobuilder" > ${D}${PTEST_PATH}/tests/10ddf-fail-stop-readd.broken
}

RDEPENDS:${PN} += "bash"
RDEPENDS:${PN}-ptest += " \
    bash \
    e2fsprogs-mke2fs \
    util-linux-lsblk \
    util-linux-losetup \
    util-linux-blockdev \
    strace \
"
RRECOMMENDS:${PN}-ptest += " \
    coreutils \
    kernel-module-loop \
    kernel-module-linear \
    kernel-module-raid0 \
    kernel-module-raid1 \
    kernel-module-raid10 \
    kernel-module-raid456 \
"

FILES:${PN} += "${systemd_unitdir}/*"

# strace is not yet ported to rv32
RDEPENDS:${PN}-ptest:remove:riscv32 = "strace"
do_install_ptest:append:riscv32 () {
    echo "disabled, no strace" > ${D}${PTEST_PATH}/tests/07revert-grow.broken
    echo "disabled, no strace" > ${D}${PTEST_PATH}/tests/07revert-inplace.broken
}
