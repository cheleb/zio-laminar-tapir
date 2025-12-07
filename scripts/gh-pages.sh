pushd examples/client
npm i
npm run build
export VERSION=`git describe --tags --abbrev=0 | sed "s/v//"`
echo "Documentation version: $VERSION"
npm run build
popd
sbt website
