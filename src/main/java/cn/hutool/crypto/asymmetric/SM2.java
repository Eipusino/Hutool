package cn.hutool.crypto.asymmetric;

import cn.hutool.core.lang.*;
import cn.hutool.core.util.*;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.*;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.digests.*;
import org.bouncycastle.crypto.engines.*;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.*;
import org.bouncycastle.util.*;
import org.bouncycastle.util.encoders.*;

import java.math.*;
import java.security.*;

/**
 * 国密SM2非对称算法实现，基于BC库<br>
 * SM2算法只支持公钥加密，私钥解密<br>
 * 参考：https://blog.csdn.net/pridas/article/details/86118774
 *
 * <p>
 * 国密算法包括：
 * <ol>
 *     <li>非对称加密和签名：SM2</li>
 *     <li>摘要签名算法：SM3</li>
 *     <li>对称加密：SM4</li>
 * </ol>
 *
 * @author looly
 * @since 4.3.2
 */
public class SM2 extends AbstractAsymmetricCrypto<SM2> {
	private static final long serialVersionUID = 1L;

	/**
	 * 算法EC
	 */
	private static final String ALGORITHM_SM2 = "SM2";

	protected SM2Engine engine;
	protected SM2Signer signer;

	private ECPrivateKeyParameters privateKeyParams;
	private ECPublicKeyParameters publicKeyParams;

	private DSAEncoding encoding = StandardDSAEncoding.INSTANCE;
	private Digest digest = new SM3Digest();
	private SM2Engine.Mode mode = SM2Engine.Mode.C1C3C2;

	// ------------------------------------------------------------------ Constructor start

	/**
	 * 构造，生成新的私钥公钥对
	 */
	public SM2() {
		this(null, (byte[]) null);
	}

	/**
	 * 构造<br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密或者解密
	 *
	 * @param privateKeyStr 私钥Hex或Base64表示，必须使用PKCS#8规范
	 * @param publicKeyStr  公钥Hex或Base64表示，必须使用X509规范
	 */
	public SM2(String privateKeyStr, String publicKeyStr) {
		this(SecureUtil.decode(privateKeyStr), SecureUtil.decode(publicKeyStr));
	}

	/**
	 * 构造 <br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密或者解密
	 *
	 * @param privateKey 私钥，可以使用PKCS#8、D值或PKCS#1规范
	 * @param publicKey  公钥，可以使用X509、Q值或PKCS#1规范
	 */
	public SM2(byte[] privateKey, byte[] publicKey) {
		this(
				ECKeyUtil.decodePrivateKeyParams(privateKey),
				ECKeyUtil.decodePublicKeyParams(publicKey)
		);
	}

	/**
	 * 构造 <br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密或者解密
	 *
	 * @param privateKey 私钥
	 * @param publicKey  公钥
	 */
	public SM2(PrivateKey privateKey, PublicKey publicKey) {
		this(BCUtil.toParams(privateKey), BCUtil.toParams(publicKey));
		if (null != privateKey) {
			this.privateKey = privateKey;
		}
		if (null != publicKey) {
			this.publicKey = publicKey;
		}
	}

	/**
	 * 构造 <br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密或者解密
	 *
	 * @param privateKeyHex      私钥16进制
	 * @param publicKeyPointXHex 公钥X16进制
	 * @param publicKeyPointYHex 公钥Y16进制
	 * @since 5.2.0
	 */
	public SM2(String privateKeyHex, String publicKeyPointXHex, String publicKeyPointYHex) {
		this(BCUtil.toSm2Params(privateKeyHex), BCUtil.toSm2Params(publicKeyPointXHex, publicKeyPointYHex));
	}

	/**
	 * 构造 <br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密或者解密
	 *
	 * @param privateKey      私钥
	 * @param publicKeyPointX 公钥X
	 * @param publicKeyPointY 公钥Y
	 * @since 5.2.0
	 */
	public SM2(byte[] privateKey, byte[] publicKeyPointX, byte[] publicKeyPointY) {
		this(BCUtil.toSm2Params(privateKey), BCUtil.toSm2Params(publicKeyPointX, publicKeyPointY));
	}

	/**
	 * 构造 <br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密或者解密
	 *
	 * @param privateKeyParams 私钥，可以为null
	 * @param publicKeyParams  公钥，可以为null
	 */
	public SM2(ECPrivateKeyParameters privateKeyParams, ECPublicKeyParameters publicKeyParams) {
		super(ALGORITHM_SM2, null, null);
		this.privateKeyParams = privateKeyParams;
		this.publicKeyParams = publicKeyParams;
		this.init();
	}

	// ------------------------------------------------------------------ Constructor end

	/**
	 * 初始化<br>
	 * 私钥和公钥同时为空时生成一对新的私钥和公钥<br>
	 * 私钥和公钥可以单独传入一个，如此则只能使用此钥匙来做加密（签名）或者解密（校验）
	 *
	 * @return this
	 */
	public SM2 init() {
		if (null == this.privateKeyParams && null == this.publicKeyParams) {
			super.initKeys();
			this.privateKeyParams = BCUtil.toParams(this.privateKey);
			this.publicKeyParams = BCUtil.toParams(this.publicKey);
		}
		return this;
	}

	@Override
	public SM2 initKeys() {
		// 阻断父类中自动生成密钥对的操作，此操作由本类中进行。
		// 由于用户可能传入Params而非key，因此此时key必定为null，故此不再生成
		return this;
	}

	// --------------------------------------------------------------------------------- Encrypt

	/**
	 * 使用公钥加密，SM2非对称加密的结果由C1,C3,C2三部分组成，其中：
	 *
	 * <pre>
	 * C1 生成随机数的计算出的椭圆曲线点
	 * C3 SM3的摘要值
	 * C2 密文数据
	 * </pre>
	 *
	 * @param data    被加密的bytes
	 * @return 加密后的bytes
	 * @throws CryptoException 包括InvalidKeyException和InvalidCipherTextException的包装异常
	 * @since 5.7.10
	 */
	public byte[] encrypt(byte[] data) throws CryptoException {
		return encrypt(data, KeyType.PublicKey);
	}

	/**
	 * 加密，SM2非对称加密的结果由C1,C3,C2三部分组成，其中：
	 *
	 * <pre>
	 * C1 生成随机数的计算出的椭圆曲线点
	 * C3 SM3的摘要值
	 * C2 密文数据
	 * </pre>
	 *
	 * @param data    被加密的bytes
	 * @param keyType 私钥或公钥 {@link KeyType}
	 * @return 加密后的bytes
	 * @throws CryptoException 包括InvalidKeyException和InvalidCipherTextException的包装异常
	 */
	@Override
	public byte[] encrypt(byte[] data, KeyType keyType) throws CryptoException {
		if (KeyType.PublicKey != keyType) {
			throw new IllegalArgumentException("Encrypt is only support by public key");
		}
		return encrypt(data, new ParametersWithRandom(getCipherParameters(keyType)));
	}

	/**
	 * 加密，SM2非对称加密的结果由C1,C2,C3三部分组成，其中：
	 *
	 * <pre>
	 * C1 生成随机数的计算出的椭圆曲线点
	 * C2 密文数据
	 * C3 SM3的摘要值
	 * </pre>
	 *
	 * @param data             被加密的bytes
	 * @param pubKeyParameters 公钥参数
	 * @return 加密后的bytes
	 * @throws CryptoException 包括InvalidKeyException和InvalidCipherTextException的包装异常
	 * @since 5.1.6
	 */
	public byte[] encrypt(byte[] data, CipherParameters pubKeyParameters) throws CryptoException {
		lock.lock();
		final SM2Engine engine = getEngine();
		try {
			engine.init(true, pubKeyParameters);
			return engine.processBlock(data, 0, data.length);
		} catch (InvalidCipherTextException e) {
			throw new CryptoException(e);
		} finally {
			lock.unlock();
		}
	}

	// --------------------------------------------------------------------------------- Decrypt

	/**
	 * 使用私钥解密
	 *
	 * @param data    SM2密文，实际包含三部分：ECC公钥、真正的密文、公钥和原文的SM3-HASH值
	 * @return 加密后的bytes
	 * @throws CryptoException 包括InvalidKeyException和InvalidCipherTextException的包装异常
	 * @since 5.7.10
	 */
	public byte[] decrypt(byte[] data) throws CryptoException {
		return decrypt(data, KeyType.PrivateKey);
	}

	/**
	 * 解密
	 *
	 * @param data    SM2密文，实际包含三部分：ECC公钥、真正的密文、公钥和原文的SM3-HASH值
	 * @param keyType 私钥或公钥 {@link KeyType}
	 * @return 加密后的bytes
	 * @throws CryptoException 包括InvalidKeyException和InvalidCipherTextException的包装异常
	 */
	@Override
	public byte[] decrypt(byte[] data, KeyType keyType) throws CryptoException {
		if (KeyType.PrivateKey != keyType) {
			throw new IllegalArgumentException("Decrypt is only support by private key");
		}
		return decrypt(data, getCipherParameters(keyType));
	}

	/**
	 * 解密
	 *
	 * @param data                 SM2密文，实际包含三部分：ECC公钥、真正的密文、公钥和原文的SM3-HASH值
	 * @param privateKeyParameters 私钥参数
	 * @return 加密后的bytes
	 * @throws CryptoException 包括InvalidKeyException和InvalidCipherTextException的包装异常
	 * @since 5.1.6
	 */
	public byte[] decrypt(byte[] data, CipherParameters privateKeyParameters) throws CryptoException {
		Assert.isTrue(data.length > 1, "Invalid SM2 cipher text, must be at least 1 byte long");
		// 检查数据，gmssl等库生成的密文不包含04前缀（非压缩数据标识），此处检查并补充
		// 参考：https://blog.csdn.net/softt/article/details/139978608
		// 根据公钥压缩形态不同，密文分为两种压缩形式：
		// C1( 03 + X ) + C3（32个字节）+ C2
		// C1( 02 + X ) + C3（32个字节）+ C2
		// 非压缩公钥正常形态为04 + X  + Y，由于各个算法库差异，04有时候会省略
		// 非压缩密文正常形态为04 + C1 + C3 + C2
		if (data[0] != 0x04 && data[0] != 0x02 && data[0] != 0x03) {
			data = (byte[]) ArrayUtil.insert(data, 0, 0x04);
		}

		lock.lock();
		final SM2Engine engine = getEngine();
		try {
			engine.init(false, privateKeyParameters);
			return engine.processBlock(data, 0, data.length);
		} catch (InvalidCipherTextException e) {
			throw new CryptoException(e);
		} finally {
			lock.unlock();
		}
	}
	// --------------------------------------------------------------------------------- Sign and Verify

	/**
	 * 用私钥对信息生成数字签名
	 *
	 * @param dataHex 被签名的数据数据
	 * @return 签名
	 */
	public String signHexFromHex(String dataHex) {
		return signHex(dataHex, null);
	}

	/**
	 * 用私钥对信息生成数字签名
	 *
	 * @param dataHex 被签名的数据数据
	 * @return 签名
	 * @deprecated 歧义，使用{@link #signHexFromHex(String)}
	 */
	@Deprecated
	public String signHex(String dataHex) {
		return signHex(dataHex, null);
	}

	/**
	 * 用私钥对信息生成数字签名
	 *
	 * @param dataHex 被签名的数据数据
	 * @param idHex   可以为null，若为null，则默认withId为字节数组:"1234567812345678".getBytes()
	 * @return 签名
	 */
	public String signHexFromHex(String dataHex, String idHex) {
		return HexUtil.encodeHexStr(sign(HexUtil.decodeHex(dataHex), HexUtil.decodeHex(idHex)));
	}

	/**
	 * 用私钥对信息生成数字签名
	 *
	 * @param dataHex 被签名的数据数据
	 * @param idHex   可以为null，若为null，则默认withId为字节数组:"1234567812345678".getBytes()
	 * @return 签名
	 * @deprecated 歧义，使用{@link #signHexFromHex(String, String)}
	 */
	@Deprecated
	public String signHex(String dataHex, String idHex) {
		return HexUtil.encodeHexStr(sign(HexUtil.decodeHex(dataHex), HexUtil.decodeHex(idHex)));
	}

	/**
	 * 用私钥对信息生成数字签名，签名格式为ASN1<br>
	 * * 在硬件签名中，返回结果为R+S，可以通过调用{@link cn.hutool.crypto.SmUtil#rsAsn1ToPlain(byte[])}方法转换之。
	 *
	 * @param data 加密数据
	 * @return 签名
	 */
	public byte[] sign(byte[] data) {
		return sign(data, null);
	}

	/**
	 * 用私钥对信息生成数字签名，签名格式为ASN1<br>
	 * 在硬件签名中，返回结果为R+S，可以通过调用{@link cn.hutool.crypto.SmUtil#rsAsn1ToPlain(byte[])}方法转换之。
	 *
	 * @param data 被签名的数据数据
	 * @param id   可以为null，若为null，则默认withId为字节数组:"1234567812345678".getBytes()
	 * @return 签名
	 */
	public byte[] sign(byte[] data, byte[] id) {
		lock.lock();
		final SM2Signer signer = getSigner();
		try {
			CipherParameters param = new ParametersWithRandom(getCipherParameters(KeyType.PrivateKey));
			if (id != null) {
				param = new ParametersWithID(param, id);
			}
			signer.init(true, param);
			signer.update(data, 0, data.length);
			return signer.generateSignature();
		} catch (org.bouncycastle.crypto.CryptoException e) {
			throw new CryptoException(e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 用公钥检验数字签名的合法性
	 *
	 * @param dataHex 的数据
	 * @param signHex 签名
	 * @return 是否验证通过
	 * @since 5.2.0
	 */
	public boolean verifyHex(String dataHex, String signHex) {
		return verifyHex(dataHex, signHex, null);
	}

	/**
	 * 用公钥检验数字签名的合法性
	 *
	 * @param data 数据
	 * @param sign 签名
	 * @return 是否验证通过
	 */
	public boolean verify(byte[] data, byte[] sign) {
		return verify(data, sign, null);
	}

	/**
	 * 用公钥检验数字签名的合法性
	 *
	 * @param dataHex 数据的Hex值
	 * @param signHex 签名的Hex值
	 * @param idHex   ID的Hex值
	 * @return 是否验证通过
	 * @since 5.2.0
	 */
	public boolean verifyHex(String dataHex, String signHex, String idHex) {
		return verify(HexUtil.decodeHex(dataHex), HexUtil.decodeHex(signHex), HexUtil.decodeHex(idHex));
	}

	/**
	 * 用公钥检验数字签名的合法性
	 *
	 * @param data 数据
	 * @param sign 签名
	 * @param id   可以为null，若为null，则默认withId为字节数组:"1234567812345678".getBytes()
	 * @return 是否验证通过
	 */
	public boolean verify(byte[] data, byte[] sign, byte[] id) {
		lock.lock();
		final SM2Signer signer = getSigner();
		try {
			CipherParameters param = getCipherParameters(KeyType.PublicKey);
			if (id != null) {
				param = new ParametersWithID(param, id);
			}
			signer.init(false, param);
			signer.update(data, 0, data.length);
			return signer.verifySignature(sign);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public SM2 setPrivateKey(PrivateKey privateKey) {
		super.setPrivateKey(privateKey);

		// 重新初始化密钥参数，防止重新设置密钥时导致密钥无法更新
		this.privateKeyParams = BCUtil.toParams(privateKey);

		return this;
	}

	/**
	 * 设置私钥参数
	 *
	 * @param privateKeyParams 私钥参数
	 * @return this
	 * @since 5.2.0
	 */
	public SM2 setPrivateKeyParams(ECPrivateKeyParameters privateKeyParams) {
		this.privateKeyParams = privateKeyParams;
		return this;
	}

	@Override
	public SM2 setPublicKey(PublicKey publicKey) {
		super.setPublicKey(publicKey);

		// 重新初始化密钥参数，防止重新设置密钥时导致密钥无法更新
		this.publicKeyParams = BCUtil.toParams(publicKey);

		return this;
	}

	/**
	 * 设置公钥参数
	 *
	 * @param publicKeyParams 公钥参数
	 * @return this
	 */
	public SM2 setPublicKeyParams(ECPublicKeyParameters publicKeyParams) {
		this.publicKeyParams = publicKeyParams;
		return this;
	}

	/**
	 * 设置DSA signatures的编码为PlainDSAEncoding
	 *
	 * @return this
	 * @since 5.3.1
	 */
	public SM2 usePlainEncoding() {
		return setEncoding(PlainDSAEncoding.INSTANCE);
	}

	/**
	 * 设置DSA signatures的编码
	 *
	 * @param encoding {@link DSAEncoding}实现
	 * @return this
	 * @since 5.3.1
	 */
	public SM2 setEncoding(DSAEncoding encoding) {
		this.encoding = encoding;
		this.signer = null;
		return this;
	}

	/**
	 * 设置Hash算法
	 *
	 * @param digest {@link Digest}实现
	 * @return this
	 * @since 5.3.1
	 */
	public SM2 setDigest(Digest digest) {
		this.digest = digest;
		this.engine = null;
		this.signer = null;
		return this;
	}

	/**
	 * 设置SM2模式，旧版是C1C2C3，新版本是C1C3C2
	 *
	 * @param mode {@link SM2Engine.Mode}
	 * @return this
	 */
	public SM2 setMode(SM2Engine.Mode mode) {
		this.mode = mode;
		this.engine = null;
		return this;
	}

	/**
	 * 获得私钥D值（编码后的私钥）
	 *
	 * @return D值
	 * @since 5.5.9
	 */
	public byte[] getD() {
		return BigIntegers.asUnsignedByteArray(32,getDBigInteger());
	}

	/**
	 * 获得私钥D值（编码后的私钥）
	 *
	 * @return D值
	 * @since 5.7.17
	 */
	public String getDHex() {
		return new String(Hex.encode(getD()));
	}

	/**
	 * 获得私钥D值
	 *
	 * @return D值
	 * @since 5.7.17
	 */
	public BigInteger getDBigInteger() {
		return this.privateKeyParams.getD();
	}

	/**
	 * 获得公钥Q值（编码后的公钥）
	 *
	 * @param isCompressed 是否压缩
	 * @return Q值
	 * @since 5.5.9
	 */
	public byte[] getQ(boolean isCompressed) {
		return this.publicKeyParams.getQ().getEncoded(isCompressed);
	}

	// ------------------------------------------------------------------------------------------------------------------------- Private method start

	/**
	 * 获取密钥类型对应的加密参数对象{@link CipherParameters}
	 *
	 * @param keyType Key类型枚举，包括私钥或公钥
	 * @return {@link CipherParameters}
	 */
	private CipherParameters getCipherParameters(KeyType keyType) {
		switch (keyType) {
			case PublicKey:
				Assert.notNull(this.publicKeyParams, "PublicKey must be not null !");
				return this.publicKeyParams;
			case PrivateKey:
				Assert.notNull(this.privateKeyParams, "PrivateKey must be not null !");
				return this.privateKeyParams;
		}

		return null;
	}

	/**
	 * 获取{@link SM2Engine}，此对象为懒加载模式
	 *
	 * @return {@link SM2Engine}
	 */
	private SM2Engine getEngine() {
		if (null == this.engine) {
			Assert.notNull(this.digest, "digest must be not null !");
			this.engine = new SM2Engine(this.digest, this.mode);
		}
		this.digest.reset();
		return this.engine;
	}

	/**
	 * 获取{@link SM2Signer}，此对象为懒加载模式
	 *
	 * @return {@link SM2Signer}
	 */
	private SM2Signer getSigner() {
		if (null == this.signer) {
			Assert.notNull(this.digest, "digest must be not null !");
			this.signer = new SM2Signer(this.encoding, this.digest);
		}
		this.digest.reset();
		return this.signer;
	}
	// ------------------------------------------------------------------------------------------------------------------------- Private method end
}
