const { UserRepository } = require("../database");
const { FormateData, GeneratePassword, GenerateSalt, GenerateSignature, ValidatePassword } = require('../utils');

class UserService {

    constructor() {
        this.repository = new UserRepository();
    }

    async SignIn(userInputs){
        const { email, password } = userInputs;
        const existingUser = await this.repository.FindUser({ email});
        if (existingUser){
            const validPassword = await ValidatePassword(password, existingUser.password, existingUser.salt);
            if (validPassword){
                const token = await GenerateSignature({ email: existingUser.email, _id: existingUser._id});
                return FormateData({id: existingUser._id, token });
            }
        }
        return FormateData(null);
    }

    async SignUp(userInputs){
        const { email, password, phone } = userInputs;
        let salt = await GenerateSalt();       
        let userPassword = await GeneratePassword(password, salt);
        const existingUser = await this.repository.CreateUser({ email, password: userPassword, phone, salt});
        const token = await GenerateSignature({ email: email, _id: existingUser._id});
        return FormateData({id: existingUser._id, token });
    }

    async AddNewAddress(_id,userInputs){
        const { street, postalCode, city,country} = userInputs;
        const addressResult = await this.repository.CreateAddress({ _id, street, postalCode, city,country})
        return FormateData(addressResult);
    }

    async GetProfile(id){
        const existingUser = await this.repository.FindUserById({id});
        return FormateData(existingUser);
    }

    async GetShopingDetails(id){
        const existingUser = await this.repository.FindUserById({id});
        if(existingUser){
            // const orders = await this.shopingRepository.Orders(id);
           return FormateData(existingUser);
        }       
        return FormateData({ msg: 'Error'});
    }

    async GetWishList(userId){
        const wishListItems = await this.repository.Wishlist(userId);
        return FormateData(wishListItems);
    }

    async AddToWishlist(userId, product){
        const wishlistResult = await this.repository.AddWishlistItem(userId, product);        
        return FormateData(wishlistResult);
    }

    async ManageCart(userId, product, qty, isRemove){
        const cartResult = await this.repository.AddCartItem(userId, product, qty, isRemove);        
        return FormateData(cartResult);
    }

    async ManageOrder(userId, order){
        const orderResult = await this.repository.AddOrderToProfile(userId, order);
        return FormateData(orderResult);
    }

    async SubscribeEvents(payload){
        console.log('Triggering.... User Events')
        payload = JSON.parse(payload)
        const { event, data } =  payload;
        const { userId, product, order, qty } = data;

        switch (event){
            case 'ADD_TO_WISHLIST':
            case 'REMOVE_FROM_WISHLIST':
                this.AddToWishlist(userId, product)
                break;
            case 'ADD_TO_CART':
                this.ManageCart(userId, product, qty, false);
                break;
            case 'REMOVE_FROM_CART':
                this.ManageCart(userId, product, qty, true);
                break;
            case 'CREATE_ORDER':
                this.ManageOrder(userId, order);
                break;
            default:
                break;
        }
    }

}

module.exports = UserService;
