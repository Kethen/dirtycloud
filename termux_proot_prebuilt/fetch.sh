set -xe

script_dir="$(dirname $0)"
script_dir="$(realpath $script_dir)"
base_url="https://packages.termux.dev/apt/apt/termux-main/pool/main/p/proot/"
package_prefix=proot_5.1.107-70_

base_url_talloc="https://packages.termux.dev/apt/apt/termux-main/pool/main/libt/libtalloc/"
package_prefix_talloc=libtalloc_2.4.3_

cd $script_dir
rm -f *.deb

for arch in x86_64 i686 arm aarch64
do
	cd "$script_dir"
	wget "${base_url}${package_prefix}${arch}.deb"
	wget "${base_url_talloc}${package_prefix_talloc}${arch}.deb"
	rm -rf $arch
	mkdir $arch
	cd $arch
	ar xv "../${package_prefix}${arch}.deb"
	tar -xf data.tar.xz
	ar xv "../${package_prefix_talloc}${arch}.deb"
	tar -xf data.tar.xz
done
